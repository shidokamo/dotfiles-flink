FLINK_HOME    := /opt/flink
FLINK_VERSION := flink-1.9.0
FLINK_SCALA_VERSION := 2.12
STARTUP_SCRIPT := $(abspath ./startup-script.sh)
FLINK_USER    := ${USER}
# FLINK_WORKER_MEMORY := 3g
# FLINKEXECUTOR_MEMORY := 3g
# PYFLINK_PYTHON := /usr/bin/python3
# PYFLINK_DRIVER_PYTHON := /usr/bin/python3
KAFKA_ENDPOINT := 172.16.130.5:31090
MACHINE       := n1-standard-1
export
WORKERS       := w0 w1

# Install Spark to master
install:
	sudo -E ${STARTUP_SCRIPT}
uninstall:
	-sudo rm -rf ${SPARK_HOME}

# Create VMs (assuming that key file already exists)
workers:${WORKERS}
${WORKERS}:
	./create-vm.sh $@
	gcloud compute scp ~/.ssh/id_rsa.pub $@:~/.ssh/authorized_keys
check-worker-log:
	for i in ${WORKERS}; do echo "----- $$i -----"; gcloud compute ssh $$i --command="grep 'startup-script.*Return code' /var/log/syslog"; done
delete-known-host:
	for i in ${WORKERS}; do ssh-keygen -f "/home/shidokamo/.ssh/known_hosts" -R "$$i"; done
delete-worker:delete-known-host
	yes Y | gcloud compute instances delete ${WORKERS}

# Update config
gen_conf:
	# echo "SPARK_MASTER_HOST=$(shell hostname)" > conf/spark-env.sh
	# echo "SPARK_WORKER_MEMORY=${SPARK_WORKER_MEMORY}" >> conf/spark-env.sh
	# echo "SPARK_EXECUTOR_MEMORY=${SPARK_EXECUTOR_MEMORY}" >> conf/spark-env.sh
	# echo "PYSPARK_PYTHON=${PYSPARK_PYTHON}" >> conf/spark-env.sh
	# echo "PYSPARK_DRIVER_PYTHON=${PYSPARK_DRIVER_PYTHON}" >> conf/spark-env.sh
config-master:gen_conf
	echo ${WORKERS} | sed 's/\s\+/\n/g' > ${FLINK_HOME}/conf/slaves
	#echo $(shell hostname) >> ${SPARK_HOME}/conf/slaves
	ssh ${USER}@localhost echo "Login test" || cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
	cp conf/* ${FLINK_HOME}/conf/
config-slave:gen_conf
	# Copy config files to workers
	for i in ${WORKERS}; do gcloud compute scp --recurse conf $$i:${FLINK_HOME}; done

# Run cluster
start-cluster:stop-cluster config-slave config-master
	${FLINK_HOME}/bin/start-master.sh
	${FLINK_HOME}/bin/start-slaves.sh
stop-cluster:
	-${FLINK_HOME}/sbin/stop-slaves.sh
	-${FLINK_HOME}/sbin/stop-master.sh

test:
	sed '1,27d' example/bible.txt | sed '99844,$$d' > tmp.txt
	${FLINK_HOME}/bin/flink run ${FLINK_HOME}/examples/batch/WordCount.jar --input example/bible.txt --output wc.out
