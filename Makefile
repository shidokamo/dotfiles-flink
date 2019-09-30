FLINK_HOME    := /opt/flink
FLINK_VERSION := flink-1.9.0
FLINK_SCALA_VERSION := 2.11
STARTUP_SCRIPT := $(abspath ./startup-script.sh)
FLINK_USER    := ${USER}
FLINK_WORKER_MEMORY := 2600m
FLINK_WORKER_CPU := 1
FLINK_WORKER_TOTAL_CPU := 2
KAFKA_ENDPOINT := 172.16.128.5:31090
MACHINE       := n1-standard-1
# FLINKEXECUTOR_MEMORY := 3g
# PYFLINK_PYTHON := /usr/bin/python3
# PYFLINK_DRIVER_PYTHON := /usr/bin/python3
export
WORKERS       := w0 w1
FLINK         := ${FLINK_HOME}/bin/flink

# Install Spark to master
install:
	sudo -E ${STARTUP_SCRIPT}
uninstall:
	-sudo rm -rf ${SPARK_HOME}

# Create VMs (assuming that key file already exists)
workers:${WORKERS}
${WORKERS}:
	./create-vm.sh $@
	-gcloud compute scp ~/.ssh/id_rsa.pub $@:~/.ssh/authorized_keys
check-worker-log:
	for i in ${WORKERS}; do echo "----- $$i -----"; gcloud compute ssh $$i --command="grep 'startup-script.*Return code' /var/log/syslog"; done
delete-known-host:
	for i in ${WORKERS}; do ssh-keygen -f "/home/shidokamo/.ssh/known_hosts" -R "$$i"; done
delete-worker:delete-known-host
	yes Y | gcloud compute instances delete ${WORKERS}

# Update config
gen_conf:
	echo "jobmanager.rpc.address: $(shell hostname)" > conf/flink-conf.yaml
	echo "taskmanager.heap.size: ${FLINK_WORKER_MEMORY}" >> conf/flink-conf.yaml
	echo "taskmanager.numberOfTaskSlots: ${FLINK_WORKER_CPU}" >> conf/flink-conf.yaml
	echo "parallelism.default: ${FLINK_WORKER_TOTAL_CPU}" >> conf/flink-conf.yaml
config-host:delete-known-host
	ssh ${USER}@localhost echo "Login test" || cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
	for i in ${WORKERS}; do gcloud compute scp ~/.ssh/id_rsa.pub $$i:~/.ssh/authorized_keys; done
config-master:gen_conf config-host
	echo ${WORKERS} | sed 's/\s\+/\n/g' > ${FLINK_HOME}/conf/slaves
	#echo $(shell hostname) >> ${SPARK_HOME}/conf/slaves
	cp conf/* ${FLINK_HOME}/conf/
config-slave:gen_conf
	# Copy config files to workers
	for i in ${WORKERS}; do gcloud compute scp --recurse conf $$i:${FLINK_HOME}; done

# Run cluster
start-cluster:stop-cluster config-slave config-master
	${FLINK_HOME}/bin/start-cluster.sh
stop-cluster:
	-${FLINK_HOME}/bin/stop-cluster.sh

test:
	sed '1,27d' data/bible.txt | sed '99844,$$d' > tmp.txt
	cp tmp.txt ${FLINK_HOME}/bible.txt
	for i in ${WORKERS}; do gcloud compute scp tmp.txt $$i:/opt/flink/bible.txt; done
	rm tmp.txt
	${FLINK} run ${FLINK_HOME}/examples/batch/WordCount.jar --input "file:${FLINK_HOME}/bible.txt" --output "file:${FLINK_HOME}/wc.out"

test-compile:
	cd sbt && sbt clean assembly
test2:test-compile
	${FLINK} run \
		-c org.example.WordCount \
		sbt/target/scala-2.11/sbt-assembly-0.1-SNAPSHOT.jar
test3:test-compile
	sed '1,27d' data/bible.txt | sed '99844,$$d' > tmp.txt
	cp tmp.txt ${FLINK_HOME}/bible.txt
	for i in ${WORKERS}; do gcloud compute scp tmp.txt $$i:/opt/flink/bible.txt; done
	rm tmp.txt
	${FLINK} run \
		-c org.example.WordCountFile \
		sbt/target/scala-2.11/sbt-assembly-0.1-SNAPSHOT.jar \
		--input "file:${FLINK_HOME}/bible.txt" \
		--output "file:${FLINK_HOME}/wc.out"
test4:test-compile
	${FLINK} run \
		-c org.example.WordCountKafka \
		sbt/target/scala-2.11/sbt-assembly-0.1-SNAPSHOT.jar \
		--broker "${KAFKA_ENDPOINT}"
test5:test-compile
	${FLINK} run \
		-c org.example.Kafka \
		sbt/target/scala-2.11/sbt-assembly-0.1-SNAPSHOT.jar \
		--broker "${KAFKA_ENDPOINT}" \
		--topic logger
