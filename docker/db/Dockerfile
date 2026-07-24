FROM rockylinux:9

RUN dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm \
 && dnf -qy module disable postgresql \
 && dnf install -y postgresql16-server postgresql16-contrib \
 && dnf clean all

ENV PATH="/usr/pgsql-16/bin:${PATH}"
ENV PGDATA=/var/lib/pgsql/16/data

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh \
 && mkdir -p "$PGDATA" \
 && chown -R postgres:postgres /var/lib/pgsql

VOLUME ["/var/lib/pgsql/16/data"]
EXPOSE 5432

USER postgres
ENTRYPOINT ["docker-entrypoint.sh"]
