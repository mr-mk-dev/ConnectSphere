FROM ubuntu:latest
LABEL authors="manish"

ENTRYPOINT ["top", "-b"]