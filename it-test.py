import subprocess
import platform
import threading
import time
import portforward

if __name__ == "__main__":
    os = platform.system()

    # Depending on the os call the right script to push the docker image to the minikube docker context
    cmd = "powershell ./push-image.ps1" if os == 'Windows' else "sh push-image.sh"
    subprocess.run(cmd, shell=True).check_returncode()

    # Deploy the webserver
    subprocess.run("kubectl apply -f webserver.yaml", shell=True)

    # Scale it down because the mount is not created yet
    subprocess.run("kubectl scale deployment webserver-deployment --replicas=0", shell=True)

    # Start the mount in a separate thread/console
    threading.Thread(target=lambda: subprocess.run("minikube mount ./test-data:/test-data")).start()

    # Scale the service up, now that we have the mount
    subprocess.run("kubectl scale deployment webserver-deployment --replicas=1", shell=True)

    # Give the webserver some time to start
    time.sleep(10)

    # open port forwarding & execute the integration test in parallel
    threading.Thread(target=lambda: portforward.forward()).start()
    time.sleep(1)
    subprocess.run("sbt it:test", shell=True)
