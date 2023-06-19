
import subprocess

if __name__ == "__main__":
    subprocess.run("kubectl delete deployment webserver-deployment")
    subprocess.run("kubectl delete service webserver")
    subprocess.run("kubectl delete deployment elasticsearch-deployment")
    subprocess.run("kubectl delete service elasticsearch")
