package com.scholarvault.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.vector.ImageVector

data class UnitCategory(val id: String, val title: String, val icon: ImageVector)

data class UnitItem(val name: String, val multiplier: Double, val rule: String = "")

object UnitConverterState {
    val Length = UnitCategory("Length", "Length", Icons.Default.Straighten)
    val Weight = UnitCategory("Weight", "Weight", Icons.Default.FitnessCenter)
    val Temperature = UnitCategory("Temperature", "Temperature", Icons.Default.Thermostat)
    val Area = UnitCategory("Area", "Area", Icons.Default.SquareFoot)
    val Volume = UnitCategory("Volume", "Volume", Icons.Default.Liquor)
    val Time = UnitCategory("Time", "Time", Icons.Default.Schedule)
    val Speed = UnitCategory("Speed", "Speed", Icons.Default.Speed)
    val Energy = UnitCategory("Energy", "Energy", Icons.Default.ElectricBolt)
    val Pressure = UnitCategory("Pressure", "Pressure", Icons.Default.MonitorWeight)
    val Data = UnitCategory("Data", "Data", Icons.Default.Storage)
    val Frequency = UnitCategory("Frequency", "Frequency", Icons.Default.Speed)
    val Density = UnitCategory("Density", "Density", Icons.Default.MonitorWeight)
    val Radiation = UnitCategory("Radiation", "Radiation", Icons.Default.Thermostat)
    val Force = UnitCategory("Force", "Force", Icons.Default.ElectricBolt)
    val Power = UnitCategory("Power", "Power", Icons.Default.Speed)

    val categories = mutableStateListOf(
        Length, Weight, Temperature, Area, Volume, Time, Speed, Energy, Pressure, Data, Frequency, Density, Radiation, Force, Power
    )

    val unitData = mutableStateMapOf<UnitCategory, MutableList<UnitItem>>(
        Length to mutableStateListOf(
            UnitItem("Meter", 1.0, "Base unit"),
            UnitItem("Kilometer", 1000.0, "1 km = 1000 m"),
            UnitItem("Centimeter", 0.01, "1 cm = 0.01 m"),
            UnitItem("Millimeter", 0.001, "1 mm = 0.001 m"),
            UnitItem("Mile", 1609.344, "1 mile ≈ 1.609 km"),
            UnitItem("Yard (Gaj)", 0.9144, "1 yard = 3 feet ≈ 0.914 m"),
            UnitItem("Foot", 0.3048, "1 foot = 12 inches ≈ 0.305 m"),
            UnitItem("Inch", 0.0254, "1 inch = 2.54 cm"),
            UnitItem("Haat (Cubit)", 0.4572, "1 haat = 1.5 feet ≈ 0.457 m"),
            UnitItem("Bighat (Span)", 0.2286, "1 bighat = 0.5 haat ≈ 0.229 m"),
            UnitItem("Angul (Finger)", 0.01905, "1 angul ≈ 0.75 inch ≈ 1.9 cm"),
            UnitItem("Yojana", 13716.0, "1 Yojana ≈ 13.72 km (Ancient Indian)"),
            UnitItem("Krosha", 3429.0, "1 Krosha = 1/4 Yojana ≈ 3.43 km"),
            UnitItem("Dhanush", 1.8288, "1 Dhanush = 4 cubits ≈ 1.83 m"),
            UnitItem("Nautical Mile", 1852.0, "1 NM = 1852 m"),
            UnitItem("Parsec", 3.085677581e16, "1 parsec ≈ 3.26 light years"),
            UnitItem("Light Year", 9.460730472e15, "1 light year ≈ 9.46 trillion km"),
            UnitItem("Astronomical Unit", 1.495978707e11, "1 AU ≈ 149.6 million km"),
            UnitItem("Micrometer", 1e-6, "1 μm = 10⁻⁶ m"),
            UnitItem("Nanometer", 1e-9, "1 nm = 10⁻⁹ m"),
            UnitItem("Angstrom", 1e-10, "1 Å = 10⁻¹⁰ m"),
            UnitItem("Sut (Thread)", 0.003175, "1 Sut = 1/8 inch (South Asian construction)"),
            UnitItem("Gunter's Chain", 20.1168, "1 chain = 66 feet (Land surveying)"),
            UnitItem("Furlong", 201.168, "1 furlong = 10 chains = 660 feet"),
            UnitItem("League", 4828.032, "1 league ≈ 3 miles")
        ),
        Weight to mutableStateListOf(
            UnitItem("Kilogram", 1.0, "Base unit"),
            UnitItem("Gram", 0.001, "1 g = 0.001 kg"),
            UnitItem("Milligram", 1e-6, "1 mg = 10⁻⁶ kg"),
            UnitItem("Metric Ton", 1000.0, "1 t = 1000 kg"),
            UnitItem("Pound", 0.453592, "1 lb ≈ 0.454 kg"),
            UnitItem("Ounce", 0.0283495, "1 oz ≈ 28.3 g"),
            UnitItem("Quintal", 100.0, "1 quintal = 100 kg"),
            UnitItem("Tola", 0.0116638, "1 tola ≈ 11.66 g (Indian subcontinent)")
        ),
        Temperature to mutableStateListOf(
            UnitItem("Celsius", 1.0, "°C"),
            UnitItem("Fahrenheit", 1.0, "°F = °C × 9/5 + 32"),
            UnitItem("Kelvin", 1.0, "K = °C + 273.15")
        ),
        Area to mutableStateListOf(
            UnitItem("Square Meter", 1.0, "Base unit"),
            UnitItem("Square Kilometer", 1e6, "1 km² = 1,000,000 m²"),
            UnitItem("Hectare", 10000.0, "1 ha = 10,000 m² ≈ 2.47 acres"),
            UnitItem("Acre", 4046.8564, "1 acre = 43,560 sq ft ≈ 4046.9 m²"),
            UnitItem("Bigha (Standard)", 1333.33, "1 bigha ≈ 1/3 acre (varies by region)"),
            UnitItem("Katha", 66.8902, "1 katha = 1/20 bigha ≈ 720 sq ft"),
            UnitItem("Decimal (Dismil)", 40.4686, "1 decimal = 1/100 acre ≈ 435.6 sq ft"),
            UnitItem("Guntha", 101.1714, "1 guntha = 1/40 acre ≈ 1089 sq ft"),
            UnitItem("Kanal", 505.857, "1 kanal = 1/8 acre (Northern India & Pakistan)"),
            UnitItem("Marla", 25.2929, "1 marla = 1/160 acre (Northern India & Pakistan)"),
            UnitItem("Cent", 40.4686, "1 cent = 1/100 acre (Southern India)"),
            UnitItem("Ground", 203.22, "1 ground ≈ 2400 sq ft (Southern India)"),
            UnitItem("Chatak", 4.18, "1 chatak ≈ 45 sq ft (Bengal)"),
            UnitItem("Rood", 1011.714, "1 rood = 1/4 acre (UK / Commonwealth)"),
            UnitItem("Square Mile", 2589988.11, "1 sq mile ≈ 2.59 km²"),
            UnitItem("Square Yard", 0.83612736, "1 sq yard = 9 sq ft"),
            UnitItem("Square Foot", 0.09290304, "1 sq ft ≈ 0.093 m²"),
            UnitItem("Square Inch", 0.00064516, "1 sq inch ≈ 6.45 cm²"),
            UnitItem("Dhur (Regional)", 16.69, "1 Dhur ≈ 182.25 sq ft (East India/Nepal)"),
            UnitItem("Lecha / Lessa (Assam)", 13.378, "1 Lecha = 144 sq ft (Assam)"),
            UnitItem("Ankanam (Regional)", 6.689, "1 Ankanam = 72 sq ft (Andhra/Tamil Nadu)"),
            UnitItem("Sarsahi", 2.787, "1 Sarsahi = 30.25 sq ft (Punjab/Haryana)")
        ),
        Volume to mutableStateListOf(
            UnitItem("Liter", 1.0, "Base unit"),
            UnitItem("Milliliter", 0.001, "1 ml = 0.001 L"),
            UnitItem("Cubic Meter", 1000.0, "1 m³ = 1000 L"),
            UnitItem("Gallon (US)", 3.78541, "1 gal ≈ 3.785 L"),
            UnitItem("Fluid Ounce (US)", 0.0295735, "1 fl oz ≈ 29.6 ml"),
            UnitItem("Cup", 0.236588, "1 cup = 8 fl oz ≈ 236.6 ml"),
            UnitItem("Tablespoon", 0.0147868, "1 tbsp ≈ 14.8 ml"),
            UnitItem("Teaspoon", 0.00492892, "1 tsp ≈ 4.9 ml")
        ),
        Time to mutableStateListOf(
            UnitItem("Second", 1.0, "Base unit"),
            UnitItem("Millisecond", 0.001, "1 ms = 0.001 s"),
            UnitItem("Minute", 60.0, "1 min = 60 s"),
            UnitItem("Hour", 3600.0, "1 h = 60 min"),
            UnitItem("Day", 86400.0, "1 day = 24 h"),
            UnitItem("Week", 604800.0, "1 week = 7 days"),
            UnitItem("Month", 2629800.0, "1 month ≈ 30.44 days"),
            UnitItem("Year", 31557600.0, "1 year ≈ 365.25 days")
        ),
        Speed to mutableStateListOf(
            UnitItem("Meter per Second", 1.0, "Base unit (m/s)"),
            UnitItem("Kilometer per Hour", 0.277778, "1 km/h ≈ 0.278 m/s"),
            UnitItem("Mile per Hour", 0.44704, "1 mph ≈ 1.609 km/h"),
            UnitItem("Knot", 0.514444, "1 knot = 1 nautical mile/h ≈ 1.852 km/h")
        ),
        Energy to mutableStateListOf(
            UnitItem("Joule", 1.0, "Base unit"),
            UnitItem("Kilojoule", 1000.0, "1 kJ = 1000 J"),
            UnitItem("Calorie", 4.184, "1 cal = 4.184 J"),
            UnitItem("Kilocalorie", 4184.0, "1 kcal = 1000 cal"),
            UnitItem("Watt-hour", 3600.0, "1 Wh = 3600 J"),
            UnitItem("Kilowatt-hour", 3600000.0, "1 kWh = 3.6 MJ"),
            UnitItem("Electronvolt", 1.602176634e-19, "1 eV ≈ 1.6 × 10⁻¹⁹ J")
        ),
        Pressure to mutableStateListOf(
            UnitItem("Pascal", 1.0, "Base unit (Pa)"),
            UnitItem("Kilopascal", 1000.0, "1 kPa = 1000 Pa"),
            UnitItem("Bar", 100000.0, "1 bar = 100,000 Pa"),
            UnitItem("Atmosphere", 101325.0, "1 atm = 101,325 Pa"),
            UnitItem("Millimeter of Mercury", 133.322, "1 mmHg ≈ 133.3 Pa"),
            UnitItem("Pound per Square Inch", 6894.76, "1 psi ≈ 6.89 kPa")
        ),
        Data to mutableStateListOf(
            UnitItem("Byte", 1.0, "Base unit"),
            UnitItem("Kilobyte (KB)", 1024.0, "1 KB = 1024 Bytes"),
            UnitItem("Megabyte (MB)", 1048576.0, "1 MB = 1024 KB"),
            UnitItem("Gigabyte (GB)", 1073741824.0, "1 GB = 1024 MB"),
            UnitItem("Terabyte (TB)", 1099511627776.0, "1 TB = 1024 GB"),
            UnitItem("Bit", 0.125, "8 bits = 1 Byte")
        ),
        Frequency to mutableStateListOf(
            UnitItem("Hertz", 1.0, "Base unit (Hz)"),
            UnitItem("Kilohertz", 1000.0, "1 kHz = 1000 Hz"),
            UnitItem("Megahertz", 1e6, "1 MHz = 10⁶ Hz"),
            UnitItem("Gigahertz", 1e9, "1 GHz = 10⁹ Hz"),
            UnitItem("RPM", 0.01666667, "1 RPM = 1/60 Hz")
        ),
        Density to mutableStateListOf(
            UnitItem("kg/m³", 1.0, "Base unit (Kilogram per Cubic Meter)"),
            UnitItem("g/cm³", 1000.0, "1 g/cm³ = 1000 kg/m³"),
            UnitItem("lb/ft³", 16.018463, "1 lb/ft³ ≈ 16.02 kg/m³"),
            UnitItem("g/mL", 1000.0, "1 g/mL = 1000 kg/m³")
        ),
        Radiation to mutableStateListOf(
            UnitItem("Sievert", 1.0, "Base unit (Sv)"),
            UnitItem("Millisievert", 0.001, "1 mSv = 0.001 Sv"),
            UnitItem("Microsievert", 1e-6, "1 μSv = 10⁻⁶ Sv"),
            UnitItem("Gray", 1.0, "1 Gy = 1 Sv (for beta/gamma radiation)"),
            UnitItem("Rad", 0.01, "1 rad = 0.01 Gy"),
            UnitItem("Rem", 0.01, "1 rem = 0.01 Sv")
        ),
        Force to mutableStateListOf(
            UnitItem("Newton", 1.0, "SI base unit of Force (N)"),
            UnitItem("Kilonewton", 1000.0, "1 kN = 1000 N"),
            UnitItem("Dyne", 1e-5, "1 dyne = 10⁻⁵ N"),
            UnitItem("Pound-force", 4.448222, "1 lbf ≈ 4.448 N"),
            UnitItem("Kilogram-force", 9.80665, "1 kgf ≈ 9.807 N")
        ),
        Power to mutableStateListOf(
            UnitItem("Watt", 1.0, "SI base unit of Power (W)"),
            UnitItem("Kilowatt", 1000.0, "1 kW = 1000 W"),
            UnitItem("Megawatt", 1e6, "1 MW = 10⁶ W"),
            UnitItem("Horsepower", 745.7, "1 mechanical horsepower ≈ 745.7 W"),
            UnitItem("Milliwatt", 0.001, "1 mW = 0.001 W"),
            UnitItem("BTU per Hour", 0.293071, "1 BTU/hr ≈ 0.293 W")
        )
    )

    fun addCustomCategory(title: String, units: List<UnitItem>) {
        val catId = title.trim().lowercase()
        val cat = UnitCategory(catId, title.trim(), Icons.Default.Extension)
        if (categories.none { it.id == cat.id }) {
            categories.add(cat)
            unitData[cat] = mutableStateListOf<UnitItem>().apply {
                addAll(units)
            }
        } else {
            val existing = categories.first { it.id == cat.id }
            unitData[existing]?.let { list ->
                list.clear()
                list.addAll(units)
            } ?: run {
                unitData[existing] = mutableStateListOf<UnitItem>().apply {
                    addAll(units)
                }
            }
        }
    }

    fun addCustomUnit(category: UnitCategory, name: String, multiplier: Double, rule: String) {
        if (unitData[category] == null) {
            unitData[category] = mutableStateListOf()
        }
        unitData[category]?.add(UnitItem(name.trim(), multiplier, rule.trim()))
    }
}

fun convertValue(value: Double, fromUnit: UnitItem, toUnit: UnitItem, category: UnitCategory): Double {
    if (category.id.equals("temperature", ignoreCase = true)) {
        val celsius = when (fromUnit.name) {
            "Fahrenheit" -> (value - 32.0) * 5.0 / 9.0
            "Kelvin" -> value - 273.15
            else -> value
        }
        return when (toUnit.name) {
            "Fahrenheit" -> (celsius * 9.0 / 5.0) + 32.0
            "Kelvin" -> celsius + 273.15
            else -> celsius
        }
    }
    val base = value * fromUnit.multiplier
    return base / toUnit.multiplier
}

