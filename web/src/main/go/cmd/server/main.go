package main

import (
	"fmt"
	"math"
	"net/http"
	"os"
	"strings"
	"time"

	_ "github.com/cBioPortal/oncotree/docs"
	"github.com/cBioPortal/oncotree/internal"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"

	logrus "github.com/sirupsen/logrus"

	swaggerFiles "github.com/swaggo/files"

	ginSwagger "github.com/swaggo/gin-swagger"
)

const (
	HeaderRequestID = "X-Request-ID"
	ContextLogger   = "logger"
)

// Use this when you want to log anything in your request
// so that you have access to the request ID and other useful information
// in the log entry.
func GetLogger(c *gin.Context) *logrus.Entry {
	logger := c.MustGet(ContextLogger).(*logrus.Entry)
	return logger
}

func Logger(logger logrus.FieldLogger, notLogged ...string) gin.HandlerFunc {
	hostname, err := os.Hostname()
	if err != nil {
		hostname = "unknown"
	}

	return func(c *gin.Context) {
		path := c.Request.URL.Path
		start := time.Now()
		clientIP := c.ClientIP()
		clientUserAgent := c.Request.UserAgent()
		referer := c.Request.Referer()

		reqID := c.GetHeader(HeaderRequestID)
		if reqID == "" {
			reqID = uuid.NewString()
			c.Request.Header.Set(HeaderRequestID, reqID)
		}
		c.Writer.Header().Set(HeaderRequestID, reqID)

		entry := logger.WithFields(logrus.Fields{
			"hostname":  hostname,
			"clientIP":  clientIP,
			"method":    c.Request.Method,
			"requestId": reqID,
			"path":      path,
			"referer":   referer,
			"userAgent": clientUserAgent,
		})
		c.Set(ContextLogger, entry)

		c.Next()
		stop := time.Since(start)
		latency := int(math.Ceil(float64(stop.Nanoseconds()) / 1000000.0))

		statusCode := c.Writer.Status()
		size := c.Writer.Size()
		dataLength := max(c.Writer.Size(), 0)
		entry = entry.WithFields(logrus.Fields{
			"latency":      latency,
			"statusCode":   statusCode,
			"responseSize": size,
			"dataLength":   dataLength,
		})

		if len(c.Errors) > 0 {
			entry.Error(c.Errors.ByType(gin.ErrorTypePrivate).String())
		} else {
			if statusCode >= http.StatusInternalServerError {
				entry.Error("Status Internal Server Error")
			} else if statusCode >= http.StatusBadRequest {
				entry.Warn("Status Bad Request")
			} else {
				entry.Info("Successful Request")
			}
		}
	}
}

// @title OncoTree API
// @version 1.0.0
// @description OncoTree API definition from MSKCC
// @BasePath /

func main() {
	// We're not going to set report caller to true because errors in line numbers will be reported as
	// coming from the logger middleware and not the actual error location.
	log := logrus.New()
	logFormat := os.Getenv("LOG_FORMAT")

	if strings.ToLower(logFormat) == "json" {
		log.SetFormatter(&logrus.JSONFormatter{
			TimestampFormat: time.RFC3339Nano,
		})
	} else {
		log.SetFormatter(&logrus.TextFormatter{
			TimestampFormat: time.RFC3339Nano,
		})
	}

	log.SetOutput(os.Stdout)

	// Need to use gin.New() instead of default so all logs will use logrus
	router := gin.New()
	router.Use(Logger(log), cors.Default(), gin.Recovery())

	gin.DefaultWriter = log.WriterLevel(logrus.InfoLevel)
	gin.DefaultErrorWriter = log.WriterLevel(logrus.ErrorLevel)

	// TODO: can we configure the static folder location?
	router.Static("/assets", "../resources/static/assets")
	router.GET("/", serveIndex)
	router.GET("/news", serveIndex)
	router.GET("/mapping", serveIndex)
	router.GET("/about", serveIndex)

	// TODO: can we configure the static folder location?
	router.GET("/news.html", func(c *gin.Context) { c.File("../resources/static/news.html") })
	router.GET("/mapping.html", func(c *gin.Context) { c.File("../resources/static/mapping.html") })
	router.GET("/about.html", func(c *gin.Context) { c.File("../resources/static/about.html") })

	// TODO: Get swagger working
	url := ginSwagger.URL("doc.json") // or use your actual doc path
	router.GET("/swagger-ui.html", serveSwaggerUIHtml)
	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler,
		ginSwagger.DocExpansion("none"),
		ginSwagger.DefaultModelsExpandDepth(-1),
		url,
	))

	// Register API routes
	router.GET("/api/versions", versionsHandler)
	router.GET("/api/tumor_types.txt", tumorTypesTxtHandler)
	router.GET("/api/mainTypes", mainTypesHandler)
	router.GET("/api/tumorTypes", tumorTypesFlatHandler)
	router.GET("/api/tumorTypes/tree", tumorTypesTreeHandler)
	router.GET("/api/tumorTypes/search/:type/:query", tumorTypesSearchHandler)

	router.Run(":8080")
}

func serveIndex(c *gin.Context) {
	c.File("../resources/static/index.html")
}
func serveSwaggerUIHtml(c *gin.Context) {
	c.Redirect(http.StatusMovedPermanently, "/swagger/index.html")
}

// @Summary Get all OncoTree versions
// @Tags TreeVersions
// @Produce json
// @Success 200 {array} Version
// @Failure 503 {string} string "Service Unavailable"
// @Router /api/versions [get]
func versionsHandler(c *gin.Context) {
	// TODO: can we configure the static folder location?
	versions, err := getTreeVersions("../../../../trees")
	if err != nil {
		c.Error(fmt.Errorf("failed to load tree versions: %w", err))
		c.String(http.StatusServiceUnavailable, "Required data source unavailable")
		return
	}

	c.JSON(http.StatusOK, versions)
}

// @Summary Get Tumor Types TXT
// @Tags TumorTypesTxt
// @Description Returns tumor types in tab-separated plain text format from a specified OncoTree JSON version
// @Produce plain
// @Param version query string false "Tree version (e.g. 2024_01_01). Defaults to latest stable."
// @Success 200 {string} string "TSV content as plain text"
// @Failure 400 {string} string "Bad Request"
// @Failure 500 {string} string "Internal Server Error"
// @Router /api/tumor_types.txt [get]
func tumorTypesTxtHandler(c *gin.Context) {
	version := c.DefaultQuery("version", GetDefaultTreeVersion())
	if !isValidVersion(version) {
		c.String(http.StatusBadRequest, "Invalid version")
		return
	}

	treeFile := fmt.Sprintf("%s.json", version)

	content, err := generateTumorTypesTSV(treeFile)

	if err != nil {
		c.Error(fmt.Errorf("failed to generate tumor types TXT: %w", err))
		c.String(http.StatusInternalServerError, "Failed to generate tumor types TXT")
		return
	}

	c.Data(http.StatusOK, "text/plain; charset=utf-8", []byte(content))
}

// MainTypes handler
// @Tags MainTypes
// @Summary Get all main tumor types
// @Produce json
// @Param version query string false "The version of tumor types. Defaults to latest stable."
// @Success 200 {array} string
// @Failure 503 {string} string "Required data source unavailable"
// @Router /api/mainTypes [get]
func mainTypesHandler(c *gin.Context) {
	version := c.DefaultQuery("version", GetDefaultTreeVersion())
	if !isValidVersion(version) {
		c.String(http.StatusBadRequest, "Invalid version")
		return
	}
	treeFile := fmt.Sprintf("%s.json", version)

	mainTypes, err := getMainTypes(treeFile)
	if err != nil {
		c.Error(fmt.Errorf("Failed to get main types for version '%s': %v", version, err))
		c.String(http.StatusInternalServerError, "Failed to retrieve main types")
		return
	}

	c.JSON(http.StatusOK, mainTypes)
}

// @Summary Get flattened tumor types list
// @Tags TumorTypes
// @Produce json
// @Param version query string false "The version of tumor types. Defaults to latest stable."
// @Success 200 {array} internal.TreeNode
// @Failure 503 {string} string "Service Unavailable"
// @Router /api/tumorTypes [get]
func tumorTypesFlatHandler(c *gin.Context) {
	version := c.DefaultQuery("version", GetDefaultTreeVersion())
	if !isValidVersion(version) {
		c.String(http.StatusBadRequest, "Invalid version")
		return
	}

	treeFile := fmt.Sprintf("%s.json", version)

	tree, err := internal.ReadTreeFromFile(treeFile)
	if err != nil {
		c.Error(fmt.Errorf("Failed to read tree file '%s': %w", treeFile, err))
		c.String(http.StatusServiceUnavailable, "Required data source unavailable")
		return
	}

	flattened := flattenTumorTypes(tree)
	c.JSON(http.StatusOK, flattened)
}

// @Summary Get nested tumor types tree
// @Tags TumorTypes
// @Produce json
// @Param version query string false "The version of tumor types. Defaults to latest stable."
// @Success 200 {object} map[string]internal.TreeNode
// @Failure 503 {string} string "Service Unavailable"
// @Router /api/tumorTypes/tree [get]
func tumorTypesTreeHandler(c *gin.Context) {
	version := c.DefaultQuery("version", GetDefaultTreeVersion())
	if !isValidVersion(version) {
		c.String(http.StatusBadRequest, "Invalid version")
		return
	}

	treeFile := fmt.Sprintf("%s.json", version)

	raw, err := os.ReadFile(internal.GetTreeFilepath(treeFile))
	if err != nil {
		c.Error(fmt.Errorf("Failed to read raw tree file '%s': %w", treeFile, err))
		c.String(http.StatusServiceUnavailable, "Required data source unavailable")
		return
	}
	c.Data(http.StatusOK, "application/json", raw)
}

// @Summary Search tumor types by type and query
// @Tags TumorTypes
// @Produce json
// @Param type path string true "Query type: code, name, mainType, etc."
// @Param query path string true "Query string"
// @Param version query string false "Tree version"
// @Param exactMatch query bool false "Exact match (default true)"
// @Success 200 {array} internal.TreeNode
// @Failure 400 {string} string "Bad Request"
// @Failure 404 {string} string "Not Found"
// @Failure 503 {string} string "Service Unavailable"
// @Router /api/tumorTypes/search/{type}/{query} [get]
func tumorTypesSearchHandler(c *gin.Context) {
	allowedParams := map[string]bool{
		"version":    true,
		"exactMatch": true,
		"levels":     true,
	}
	for param := range c.Request.URL.Query() {
		if !allowedParams[param] {
			c.String(http.StatusBadRequest, fmt.Sprintf("Unexpected query parameter: '%s'", param))
			return
		}
	}

	queryType := c.Param("type")
	queryStr := c.Param("query")
	version := c.DefaultQuery("version", GetDefaultTreeVersion())
	if !isValidVersion(version) {
		c.String(http.StatusBadRequest, "Invalid version")
		return
	}
	exactMatch := c.DefaultQuery("exactMatch", "true") == "true"
	levelsStr := c.DefaultQuery("levels", "1,2,3,4,5,6,7")

	treeFile := fmt.Sprintf("%s.json", version)

	tree, err := internal.ReadTreeFromFile(treeFile)
	if err != nil {
		c.Error(fmt.Errorf("Failed to read tree file: %w", err))
		c.String(http.StatusServiceUnavailable, "Required data source unavailable")
		return
	}

	flattened := flattenTumorTypes(tree)
	results := searchTumorTypes(flattened, queryType, queryStr, exactMatch, levelsStr)

	if len(results) == 0 {
		c.String(http.StatusNotFound, "No tumor types found matching query")
		return
	}

	c.JSON(http.StatusOK, results)
}
