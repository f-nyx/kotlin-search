package be.rlab.training

import be.rlab.search.IndexManager
import be.rlab.search.query.term
import be.rlab.support.csv.Field
import be.rlab.support.csv.Parser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class DataSetLoader(
    protected val indexManager: IndexManager
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DataSetLoader::class.java)
        private const val PROBE_FIELD: String = "INITIALIZED"
    }

    protected fun loadIfRequired(
        dataSet: DataSet,
        callback: (List<Field>) -> Unit
    ) {
        val dataSetName: String = dataSet.file.nameWithoutExtension

        logger.info("verifying if data set $dataSetName is already loaded")

        val exists = indexManager.find(dataSet.namespace, dataSet.language, limit = 1) {
            term("$dataSetName::$PROBE_FIELD", "true")
        }.toList().isNotEmpty()

        if (exists) {
            logger.info("data set $dataSetName is already loaded")
            return
        }

        logger.info("data set $dataSetName is not loaded, parsing")

        Parser(dataSet.parserConfig).parse(dataSet.file.absolutePath) { _, record ->
            callback(record)
        }

        logger.info("synchronizing index")

        indexManager.index(dataSet.namespace, dataSet.language) {
            string("$dataSetName::$PROBE_FIELD", "true")
        }

        indexManager.sync()
    }
}