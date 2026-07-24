FROM rockylinux:9

RUN dnf install -y dnf-plugins-core \
 && dnf config-manager --add-repo https://pkg.cloudflare.com/cloudflared.repo \
 && dnf install -y cloudflared \
 && dnf clean all

ENTRYPOINT ["cloudflared"]
