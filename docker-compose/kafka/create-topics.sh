echo -e 'Creating kafka topics'
/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9093 --create --if-not-exists --topic player --replication-factor 1 --partitions 5
/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9093 --create --if-not-exists --topic playerError --replication-factor 1 --partitions 1

echo -e 'Successfully created the following topics:'
/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9093 --list

echo -e 'Loading players data'
/opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9093 --topic player < /tmp/player-data-full-2025-june_edited.csv

echo -e 'Data loaded successfully'