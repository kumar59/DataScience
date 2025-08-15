import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuelBurnCalculator {
	private static final double EARTH_RADIUS_NM = 3440.065; // nautical miles

	private static final Airport JFK = new Airport("JFK", 40.6413, -73.7781);
	private static final Airport LHR = new Airport("LHR", 51.4700, -0.4543);

	public static void main(String[] args) {
		Map<String, String> options = parseArgs(args);

		// Presets for common long-haul aircraft
		Map<String, Aircraft> presets = buildAircraftPresets();

		String aircraftKey = options.getOrDefault("aircraft", "A350-900");
		Aircraft aircraft = presets.getOrDefault(aircraftKey.toUpperCase(Locale.ROOT), presets.get("A350-900"));

		double cruiseSpeedKt = getDouble(options, "speedKt", aircraft.cruiseSpeedKt);
		double fuelBurnKgPerHour = getDouble(options, "fuelBurnKgPerHour", aircraft.fuelBurnKgPerHour);
		double avgWindComponentKt = getDouble(options, "windKt", 0.0); // +tailwind, -headwind
		double routeFactor = getDouble(options, "routeFactor", 1.03); // accounts for route inefficiencies
		double taxiFuelKg = getDouble(options, "taxiKg", 500.0);
		double reserveMinutes = getDouble(options, "reserveMinutes", 45.0);
		double contingencyPercent = getDouble(options, "contingencyPct", 0.05);

		// Distance JFK->LHR using great-circle
		double gcDistanceNm = greatCircleDistanceNm(JFK.latitudeDeg, JFK.longitudeDeg, LHR.latitudeDeg, LHR.longitudeDeg);
		double plannedDistanceNm = gcDistanceNm * routeFactor;

		double groundspeedKt = cruiseSpeedKt + avgWindComponentKt;
		if (groundspeedKt <= 0) {
			System.err.println("Error: Computed groundspeed is non-positive. Adjust speed or wind.");
			System.exit(1);
		}

		double cruiseHours = plannedDistanceNm / groundspeedKt;
		double cruiseFuelKg = cruiseHours * fuelBurnKgPerHour;
		double reserveFuelKg = (reserveMinutes / 60.0) * fuelBurnKgPerHour;
		double contingencyFuelKg = contingencyPercent * (cruiseFuelKg + taxiFuelKg);
		double totalFuelKg = taxiFuelKg + cruiseFuelKg + reserveFuelKg + contingencyFuelKg;

		// Output
		System.out.println("NYC (JFK) -> London (LHR) Fuel Burn Estimate");
		System.out.println("------------------------------------------------");
		System.out.printf(Locale.ROOT, "Aircraft: %s\n", aircraft.name);
		System.out.printf(Locale.ROOT, "Cruise speed: %.0f kt\n", cruiseSpeedKt);
		System.out.printf(Locale.ROOT, "Fuel burn: %.0f kg/h\n", fuelBurnKgPerHour);
		System.out.printf(Locale.ROOT, "Average wind: %+,.0f kt (tailwind positive)\n", avgWindComponentKt);
		System.out.printf(Locale.ROOT, "Route factor: %.3f\n", routeFactor);
		System.out.println();

		System.out.printf(Locale.ROOT, "Great-circle distance: %,.0f nm\n", gcDistanceNm);
		System.out.printf(Locale.ROOT, "Planned distance:      %,.0f nm\n", plannedDistanceNm);
		System.out.printf(Locale.ROOT, "Groundspeed:           %,.0f kt\n", groundspeedKt);
		System.out.printf(Locale.ROOT, "Cruise time:           %,.2f h\n", cruiseHours);
		System.out.println();

		System.out.printf(Locale.ROOT, "Taxi fuel:        %,.0f kg\n", taxiFuelKg);
		System.out.printf(Locale.ROOT, "Cruise fuel:      %,.0f kg\n", cruiseFuelKg);
		System.out.printf(Locale.ROOT, "Reserve fuel:     %,.0f kg (%.0f min)\n", reserveFuelKg, reserveMinutes);
		System.out.printf(Locale.ROOT, "Contingency fuel: %,.0f kg (%.0f%%)\n", contingencyFuelKg, contingencyPercent * 100.0);
		System.out.println("----------------------------------------");
		System.out.printf(Locale.ROOT, "Total fuel:       %,.0f kg (%.2f t)\n", totalFuelKg, totalFuelKg / 1000.0);

		System.out.println();
		System.out.println("Tip: Override defaults, e.g. --aircraft=B777-300ER --windKt=80 --routeFactor=1.05");
	}

	private static Map<String, Aircraft> buildAircraftPresets() {
		Map<String, Aircraft> presets = new HashMap<>();
		presets.put("A350-900", new Aircraft("A350-900", 470, 5800));
		presets.put("B777-300ER", new Aircraft("B777-300ER", 480, 6800));
		presets.put("B787-9", new Aircraft("B787-9", 480, 5400));
		return presets;
	}

	private static double getDouble(Map<String, String> options, String key, double defaultValue) {
		String v = options.get(key);
		if (v == null) return defaultValue;
		try {
			return Double.parseDouble(v);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number for --" + key + ": " + v);
		}
	}

	private static Map<String, String> parseArgs(String[] args) {
		Map<String, String> map = new HashMap<>();
		Pattern kvEq = Pattern.compile("--([A-Za-z0-9]+)=(.*)");
		for (int i = 0; i < args.length; i++) {
			String a = args[i];
			Matcher m = kvEq.matcher(a);
			if (m.matches()) {
				map.put(m.group(1), m.group(2));
				continue;
			}
			if (a.startsWith("--")) {
				String key = a.substring(2);
				String value = (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[++i] : "true";
				map.put(key, value);
			}
		}
		return map;
	}

	private static double greatCircleDistanceNm(double lat1Deg, double lon1Deg, double lat2Deg, double lon2Deg) {
		double lat1 = Math.toRadians(lat1Deg);
		double lon1 = Math.toRadians(lon1Deg);
		double lat2 = Math.toRadians(lat2Deg);
		double lon2 = Math.toRadians(lon2Deg);

		double dLat = lat2 - lat1;
		double dLon = lon2 - lon1;
		double sinLat = Math.sin(dLat / 2);
		double sinLon = Math.sin(dLon / 2);
		double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_NM * c;
	}

	private static final class Airport {
		final String code;
		final double latitudeDeg;
		final double longitudeDeg;
		Airport(String code, double latitudeDeg, double longitudeDeg) {
			this.code = code;
			this.latitudeDeg = latitudeDeg;
			this.longitudeDeg = longitudeDeg;
		}
	}

	private static final class Aircraft {
		final String name;
		final double cruiseSpeedKt; // indicated as TAS approximation
		final double fuelBurnKgPerHour; // average block fuel burn per hour
		Aircraft(String name, double cruiseSpeedKt, double fuelBurnKgPerHour) {
			this.name = name;
			this.cruiseSpeedKt = cruiseSpeedKt;
			this.fuelBurnKgPerHour = fuelBurnKgPerHour;
		}
	}
}