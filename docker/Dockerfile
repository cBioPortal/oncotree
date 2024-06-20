# This will copy a pre-built oncotree jar into an image.
# This requires the user to have application.properties set for ehcache settings.
# Additional properties for startup need to be passed as runtime args (e.g. graph schema)
#
# Use from root directory of repo like:
#
# docker build -f docker/Dockerfile -t oncotree-container:oncotree-tag-name . 

FROM openjdk:8-jdk-alpine

# Install autossh
RUN apk add --no-cache autossh

# Add the SSH configuration file
RUN mkdir -p /root/.ssh
RUN chmod 700 /root/.ssh
RUN echo "Host *" > /root/.ssh/config && \
    echo "    StrictHostKeyChecking no" >> /root/.ssh/config && \
    echo "    UserKnownHostsFile=/dev/null" >> /root/.ssh/config

COPY web/target/oncotree.jar oncotree.jar
