spark-submit \
	--class io.skiper.driver.Stage2StreamingDriver \
	--master spark://localhost:7077 \
	--name "demo-spark-analysis-s2" \
	--deploy-mode client \
	--driver-memory 1g \
	--executor-memory 1g \
	--total-executor-cores 2 \
	--executor-cores 2 \
	./demo-streaming/target/demo-streaming-1.0-SNAPSHOT-jar-with-dependencies.jar