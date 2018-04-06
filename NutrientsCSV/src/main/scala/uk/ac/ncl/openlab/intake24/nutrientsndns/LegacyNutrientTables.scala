package uk.ac.ncl.openlab.intake24.nutrientsndns

object LegacyNutrientTables {

  import CsvNutrientTableParser.{ excelColumnToOffset => col }

  private val ndnsCsvNutrientMapping: Map[Long, Int] =
    Map(
      1l -> col("Z"),
      2l -> col("AB"),
      8l -> col("N"),
      9l -> col("P"),
      10l -> col("R"),
      11l -> col("T"),
      13l -> col("X"),
      15l -> col("AF"),
      16l -> col("AH"),
      20l -> col("AD"),
      21l -> col("AJ"),
      22l -> col("AL"),
      23l -> col("AN"),
      24l -> col("AP"),
      25l -> col("AR"),
      26l -> col("AT"),
      27l -> col("AV"),
      28l -> col("AX"),
      29l -> col("AZ"),
      30l -> col("U"),
      49l -> col("V"),
      50l -> col("BD"),
      55l -> col("BF"),
      56l -> col("BH"),
      57l -> col("BJ"),
      58l -> col("BL"),
      59l -> col("BN"),
      114l -> col("BP"),
      115l -> col("BR"),
      116l -> col("BT"),
      117l -> col("BV"),
      119l -> col("BX"),
      120l -> col("BZ"),
      122l -> col("CB"),
      123l -> col("CD"),
      124l -> col("CF"),
      125l -> col("CH"),
      126l -> col("CJ"),
      128l -> col("CL"),
      129l -> col("CN"),
      130l -> col("CP"),
      132l -> col("CR"),
      133l -> col("CT"),
      134l -> col("CV"),
      136l -> col("CX"),
      137l -> col("CZ"),
      138l -> col("DB"),
      139l -> col("DD"),
      140l -> col("DF"),
      141l -> col("DH"),
      142l -> col("DJ"),
      143l -> col("DL"),
      144l -> col("DN"),
      145l -> col("DP"),
      146l -> col("DR"),
      147l -> col("DT"),
      148l -> col("DV"),
      149l -> col("DX"),
      151l -> col("DZ"),
      152l -> col("EB"))

  val ndnsCsvTableMapping = CsvNutrientTableMapping(1, 0, 1, None, ndnsCsvNutrientMapping)

  private val nzCsvNutrientMapping: Map[Long, Int] = Map(
    1l -> col("X"),
    2l -> col("Z"),
    3l -> col("Y"),
    4l -> col("AB"),
    5l -> col("AC"),
    6l -> col("AD"),
    7l -> col("AE"),
    8l -> col("CH"),
    9l -> col("BM"),
    11l -> col("BP"),
    13l -> col("I"),
    14l -> col("K"),
    17l -> col("AU"),
    18l -> col("AV"),
    19l -> col("AW"),
    20l -> col("D"),
    21l -> col("BU"),
    22l -> col("BW"),
    25l -> col("BC"),
    26l -> col("BA"),
    27l -> col("BV"),
    28l -> col("BH"),
    29l -> col("BF"),
    49l -> col("AF"),
    50l -> col("AS"),
    51l -> col("AO"),
    52l -> col("AP"),
    58l -> col("AT"),
    59l -> col("R"),
    60l -> col("AG"),
    61l -> col("AH"),
    62l -> col("AI"),
    63l -> col("AJ"),
    64l -> col("AK"),
    65l -> col("AL"),
    66l -> col("AM"),
    67l -> col("AN"),
    68l -> col("AQ"),
    69l -> col("AR"),
    114l -> col("BQ"),
    116l -> col("E"),
    117l -> col("L"),
    118l -> col("M"),
    121l -> col("CB"),
    122l -> col("CF"),
    123l -> col("BX"),
    124l -> col("BR"),
    127l -> col("CA"),
    128l -> col("BK"),
    129l -> col("CE"),
    131l -> col("CG"),
    132l -> col("CD"),
    133l -> col("CC"),
    134l -> col("AY"),
    135l -> col("AX"),
    138l -> col("BT"),
    139l -> col("BO"),
    140l -> col("P"),
    141l -> col("BG"),
    142l -> col("BN"),
    143l -> col("BE"),
    146l -> col("S"),
    147l -> col("CI"),
    150l -> col("BD"),
    151l -> col("BI"),
    152l -> col("BS"),
    155l -> col("F"),
    156l -> col("N"),
    157l -> col("G"),
    158l -> col("O"),
    159l -> col("Q"),
    160l -> col("T"),
    161l -> col("BB"),
    162l -> col("U"),
    163l -> col("AZ"),
    164l -> col("BJ"),
    165l -> col("BY"),
    167l -> col("H"),
    168l -> col("J"),
    169l -> col("BZ"),
    173l -> col("V"))

  val nzCsvTableMapping = CsvNutrientTableMapping(2, 0, 2, None, nzCsvNutrientMapping)
}