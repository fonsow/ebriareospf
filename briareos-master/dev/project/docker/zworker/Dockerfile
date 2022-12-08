FROM python:3.8-slim

WORKDIR /app

ADD . /app

RUN ./install-requirements.sh

CMD ["./zworker"]
