CURRENT_DIR:=$(shell pwd)

test:
	sbt test

docker: dockerimage
	docker exec -it myrvcpu bash ||\
	docker start -i myrvcpu ||\
	docker run -it --name myrvcpu -v $(CURRENT_DIR):/app myrvcpu

dockerimage:
	docker build . -t myrvcpu

.PHONY: all test docker dockerimage
