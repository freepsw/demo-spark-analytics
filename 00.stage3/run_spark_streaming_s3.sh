spark-submit \
	--class io.skiper.driver.Stage3StreamingDriver \
	--master spark://localhost:7077 \
	--name "demo-spark-analysis-s3" \
	--deploy-mode client \
	--driver-memory 1g \
	--executor-memory 1g \
	--total-executor-cores 2 \
	--executor-cores 2 \
	../00.stage2/demo-streaming/target/demo-streaming-1.0-SNAPSHOT.jar