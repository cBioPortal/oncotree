package main

import (
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

// @title OncoTree API
// @version 1.0.0
// @description OncoTree API definition from MSKCC
// @BasePath /

func main() {
	router := gin.Default()
	router.Use(cors.Default())
	router.Static("/assets", "./web/src/main/resources/static/assets")

	router.GET("/news", serveIndex)
	router.GET("/mapping", serveIndex)
	router.GET("/about", serveIndex)
	router.GET("/news.html", serveNewsIndex)

	router.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))
	//router.GET("/api/versions", api.GetVersionsHandler)

	// Start server on port 8080
	router.Run(":8080")
}

func serveIndex(c *gin.Context) {
	c.File("./web/src/main/resources/static/index.html")
}
func serveNewsIndex(c *gin.Context) {
	c.File("./web/src/main/resources/static/news.html")
}
