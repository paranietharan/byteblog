FROM ubuntu:latest
LABEL authors="paranietharan"

ENTRYPOINT ["top", "-b"]