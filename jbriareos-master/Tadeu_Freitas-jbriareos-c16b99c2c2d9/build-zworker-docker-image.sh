docker build -f ./src/docker/zworker/Dockerfile -t zworker_java .

# shellcheck disable=SC2046
docker rmi -f $(docker images -f dangling=true -q )