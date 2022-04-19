test:
	sbt test

docker:
	docker exec -it myrvcpu bash ||\
	docker start -i myrvcpu ||\
	docker run -it --name myrvcpu -v $(pwd):/app riscv/mycpu

dockerimage:
	docker build . -t myrvcpu

.PHONY: all test docker dockerimage
