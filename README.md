# Observe

This set of modules contains a web server and the client for the Observe tool

## Web backend

The backend is written with [http4s](http://http4s.org), it exposes a REST API and can provide static files for the UI. It is intended to run in the same process as the observe-server

## sbt-revolver

This project uses an extra plugin

- [sbt-revolver](https://github.com/spray/sbt-revolver): This plugin allows to restart the web server and trigger a recompilation when the source code changes

## How to compile and start the server (Good for backend development)

Go to the JVM project

```
    project observe_web_server
    ~reStart
```

Now every time a file is changed in the server code, the server files will be compiled, then the server will restart

By default the REST backend will run on port 7070

It can be stopped by executing

```
   reStop
```

from within the project.

# Observe Web Client

This module contains a web-based observe client. It contains a SPA (Single-page application) which communicates to the backend using Ajax-style calls and websockets

# How to run/develop the client

<!-- For the common case we want to develop the client but we also need to run the backend.

an sbt task

```
startObserveAll
```

Will do the following:

- Launch the backend on the background
- Pack the client going through scala.js and webpack
- Launch webpack-dev-server with a proxy to the backend

Now you can open the client at

http://localhost:8081

if you want to update the client and get automatic reload do in sbt:

```
    project observe_web_client
    ~fastOptJS
```

and to stop all the processes you can do

```
stopObserveAll
``` -->

# Deployment

Deployment is done via Docker images.

When a PR is merged into `main`, CI automatically builds a Docker image and pushes it to:

- Dockerhub, at [https://hub.docker.com/repository/docker/noirlab/gpp-obs/general](https://hub.docker.com/repository/docker/noirlab/gpp-obs/general) (private repo).
- Heroku staging environment, accessible at [https://observe-staging.lucuma.xyz](https://observe-staging.lucuma.xyz). The image is also released and the latest version should become available instantly.

## Test and Production

To deploy an image to these enviornments, you must log into the target machine and pull the `latest` image (or the specific version you want to deploy) from Noirlab's account on Dockerhub.

This requires that you

```
docker login
```

first with the `nlsoftware` account.

## Manually deploying to Staging (optional)

If, for some reason, you want to deploy to staging manually directly from your develpment environment, do the following:

- Make sure you have both `docker` and `heroku` CLIs installed and working.

- If you haven't already, run:

```
heroku login
heroku container:login
```

This will give your system access to Heroku's Docker registry.

- To deploy to Heroku, run:

```
sbt deploy/docker:publishLocal
docker tag noirlab/gpp-obs registry.heroku.com/observe-staging/web
docker push registry.heroku.com/observe-staging/web
```

This will build and push the image to Heroku's Docker registry, but it won't publish it yet.

- To publish it, run from the shell:

```
heroku container:release web -a observe-staging
```

The new version should be accessible now at [https://observe-staging.lucuma.xyz](https://observe-staging.lucuma.xyz).

# Running in Test and Production

In order for these images to run, we must pass site-specific configuration to the server. For this, the server expects a directory called `conf/local` to be mounted in the container. A local directory must be [bind mounted](https://docs.docker.com/storage/bind-mounts/) into the container, providing a local `app.conf`.

For example, assuming you have a local directory `/opt/observe/local` with a file `app.conf` with the following content:

```
environment = PRODUCTION
site = GN

lucuma-sso {
  service-token = "<INSERT TOKEN HERE>"
}

web-server {
    external-base-url = "observe.hi.gemini.edu"
    tls {
        key-store = "conf/local/cacerts.jks.dev"
        key-store-pwd = "passphrase"
        cert-pwd = "passphrase"
    }
}

etc...
```

You can run the container with the following command:

```
docker run -p 443:9090 --mount type=bind,src=/opt/observe/local,dst=/opt/docker/conf/local noirlab/gpp-obs:latest
```

Notes:

- The SSL port is by default 9090 but can be overriden by specifying the `PORT` environment variable. This port must be exposed in the container.

- To generate a service token, see the [lucuma-sso documentation](https://github.com/gemini-hlsw/lucuma-sso?tab=readme-ov-file#obtaining-a-service-jwt).

- Templates for configuration for each server (environment+site combination) are provided in `deploy/confs`. The service token is omitted from the templates in order to avoid the need to manually edit them. The service token can be passed to the container via the `SSO_SERVICE_JWT` environment variable. In the example above, this would be:

```
docker run -p 443:9090 -e SSO_SERVICE_JWT=<service-token> --mount type=bind,src=/opt/observe/local,dst=/opt/docker/conf/local noirlab/gpp-obs:latest
```
