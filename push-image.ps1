& minikube -p minikube docker-env --shell powershell | Invoke-Expression
sbt docker:publishLocal
