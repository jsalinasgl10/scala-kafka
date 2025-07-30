package kafka.parser

import com.github.tototoshi.csv.{CSVParser, DefaultCSVFormat, QUOTE_NONE, Quoting}
import kafka.error.AppExceptions.CsvParseException

object CsvParser {
  private object CsvFormat extends DefaultCSVFormat {
    override val delimiter        = ';'
    override val quoting: Quoting = QUOTE_NONE
  }

  private[this] val csvParser = new CSVParser(CsvFormat)

  def parse(line: String): List[String] =
    csvParser
      .parseLine(line)
      .getOrElse(throw CsvParseException(s"empty result parsing message: $line"))
}
