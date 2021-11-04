# This will copy a pre-built oncotree jar into an image.
# This requires the user to have application.properties set for ehcache settings.
# Additional properties for startup need to be passed as runtime args (e.g. graph schema)
#
# Use from root directory of repo like:
#
# docker build -f docker/Dockerfile -t oncotree-container:oncotree-tag-name . 

FROM openjdk:8-jdk-alpine
COPY web/target/oncotree.jar oncotree.jar
