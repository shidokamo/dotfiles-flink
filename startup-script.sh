#!/bin/sh
if [ -e FLINK_INSTALLED ]; then
  echo "FLINK is already installed."
  exit
fi

echo '----- Update system -----'
apt-get update
apt-get -y upgrade

echo '----- Setup NTP ----'
cat <<EOF > /etc/systemd/timesyncd.conf
[Time]
NTP=metadata.google.internal
EOF
systemctl daemon-reload
systemctl enable systemd-timesyncd
systemctl start systemd-timesyncd

echo '----- Install Java -----'
apt-get install -y openjdk-8-jdk

echo '----- Install Python -----'
apt-get install -y python3-pip python3-dev
apt-get install -y python-pip python-dev
## Use the latest PIP
curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python3 get-pip.py
# python3 -m pip install pyspark

echo '----- Install Flink -----'
cd /tmp
wget http://apache.cs.utah.edu/flink/${FLINK_VERSION}/${FLINK_VERSION}-bin-scala_2.11.tgz
tar xvzf ${FLINK_VERSION}-bin-scala_2.11.tgz
rm ${FLINK_VERSION}-bin-scala_2.11.tgz
mkdir -p ${FLINK_HOME}
mv ${FLINK_VERSION}-bin-scala_2.11.tgz/* ${FLINK_HOME}/

# Change owner and add full access
chown ${FLINK_USER} -R ${FLINK_HOME}
chmod 755 -R ${FLINK_HOME}

# Prevent next execution
echo "This is a flag file to prevent repeated installation." > FLINK_INSTALLED
