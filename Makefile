FLINK_HOME    := /opt/flink
FLINK_VERSION := flink-1.9.0
FLINK_SCALA_VERSION := 2.12
STARTUP_SCRIPT := $(abspath ./startup-script.sh)
FLINK_USER    := ${USER}
FLINK_WORKER_MEMORY := 3400m
FLINK_WORKER_CPU := 1
FLINK_WORKER_TOTAL_CPU := 2
KAFKA_ENDPOINT := 172.16.130.5:31090
MACHINE       := n1-standard-1
# FLINKEXECUTOR_MEMORY := 3g
# PYFLINK_PYTHON := /usr/bin/python3
# PYFLINK_DRIVER_PYTHON := /usr/bin/python3
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
	sed '1,27d' example/bible.txt | sed '99844,$$d' > tmp.txt
	${FLINK_HOME}/bin/flink run ${FLINK_HOME}/examples/batch/WordCount.jar --input example/bible.txt --output wc.out
