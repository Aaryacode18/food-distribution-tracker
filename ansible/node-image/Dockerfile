# The clean target node Ansible configures.
#
# Deliberately bare: python3 so Ansible can run modules on it, curl so a
# health check has something to make requests with, and systemd so Tomcat
# is managed as a real service (systemctl start/enable) rather than a
# hand-started java process. The JDK, Tomcat and the application are NOT
# in this image - they are what the playbook is for.
FROM ubuntu:24.04
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 curl ca-certificates systemd systemd-sysv \
    && rm -rf /var/lib/apt/lists/*
STOPSIGNAL SIGRTMIN+3
CMD ["/sbin/init"]
