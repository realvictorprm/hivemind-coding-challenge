
import subprocess

if __name__ == "__main__":
    subprocess.run("kubectl delete deployment webserver-deployment")
    subprocess.run("kubectl apply -f webserver.yaml", shell=True)
