import subprocess
import re

pattern = r"webserver-deployment-.+?-.+?\b"

if __name__ == "__main__":
    res = subprocess.run("kubectl get pods", stdout=subprocess.PIPE)
    it = str(res.stdout)
    results = re.findall(pattern, it)
    subprocess.run(f"kubectl port-forward {results[0]} 8080:8080")
