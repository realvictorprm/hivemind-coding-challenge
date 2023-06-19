eval $(minikube docker-env)
sbt docker:publishLocal
