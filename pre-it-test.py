import subprocess

if __name__ == "__main__":
    subprocess.run("kubectl apply -f elasticsearch.yaml")
