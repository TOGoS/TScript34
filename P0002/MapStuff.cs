using System.Collections.Generic;
using System.Collections.ObjectModel;

using Exception = System.Exception;
using JavaScriptConverter = System.Web.Script.Serialization.JavaScriptConverter;
using JavaScriptSerializer = System.Web.Script.Serialization.JavaScriptSerializer;
using Math = System.Math;
using Type = System.Type;

namespace TOGoS.TScrpt34_2.MapStuff {
	readonly struct PointInfo<Pos,Dat> {
		public readonly Pos Position;
		public readonly Dat Data;
		public PointInfo(Pos position, Dat data) {
			this.Position = position;
			this.Data = data;
		}
		public override string ToString() {
			return "PointInfo{ "+Position+", "+Data+"}";
		}
	}
	readonly struct LatLongPosition {
		public readonly double Latitude;
		public readonly double Longitude;
		public LatLongPosition(double lat, double longit) {
			this.Latitude = lat;
			this.Longitude = longit;
		}
		public override string ToString() {
			return "latitude: "+Latitude+", longitude: "+Longitude;
		}
	}
	readonly struct XYPosition {
		public readonly double X;
		public readonly double Y;
		public XYPosition(double x, double y) {
			this.X = x;
			this.Y = y;
		}
		public override string ToString() {
			return "x: "+X+", y: "+Y;
		}
	}
	readonly struct VegData {
		public readonly string KindName;
		public VegData(string kindName) {
			this.KindName = kindName;
		}
		public override string ToString() {
			return "kind: "+KindName;
		}
	}
	
	class PointInfoConverter<Pos,Dat> : JavaScriptConverter {
		public override IEnumerable<Type> SupportedTypes {
			//Define the ListItemCollection as a supported type.
			get { return new ReadOnlyCollection<Type>(new List<Type>(new Type[] { typeof(PointInfo<LatLongPosition,VegData>) })); }
		}
		
		protected Pos DerserializePos(IDictionary<string, object> dictionary, JavaScriptSerializer serializer) {
			if( typeof(Pos) == typeof(LatLongPosition) ) {
				return (Pos) (object) new LatLongPosition(
					serializer.ConvertToType<double>(dictionary["latitude"]),
					serializer.ConvertToType<double>(dictionary["longitude"])
				);
			} else if( typeof(Pos) == typeof(XYPosition) ) {
				return (Pos) (object) new XYPosition(
					serializer.ConvertToType<double>(dictionary["x"]),
					serializer.ConvertToType<double>(dictionary["y"])
				);
			} else {
				throw new Exception("Unsupported position type cannot be deserialized: "+typeof(Pos));
			}
		}

		public override object Deserialize(IDictionary<string, object> dictionary, Type type, JavaScriptSerializer serializer) {
			if (type == typeof(PointInfo<LatLongPosition,VegData>)) {
				return new PointInfo<Pos,VegData>(
					DerserializePos(dictionary, serializer),
					new VegData(serializer.ConvertToType<string>(dictionary["kind"]))
				);
			}	
			return null;
		}
		
		public override IDictionary<string,object> Serialize(object obj, JavaScriptSerializer serializer) {
			throw new Exception("boo");
		}
	}
	
	class JsonDecoder<Pos,Dat> {
		public IList<PointInfo<Pos,Dat>> Decode(string json) {
			var jser = new JavaScriptSerializer();
			// TODO: Something like this;
			// https://learn.microsoft.com/en-us/dotnet/api/system.web.script.serialization.javascriptserializer.registerconverters?view=netframework-4.8.1
			jser.RegisterConverters(new JavaScriptConverter[] {
				new PointInfoConverter<Pos,Dat>()
			});
			return jser.Deserialize<List<PointInfo<Pos,Dat>>>(json);
		}
	}

	class LatLongToXYConverter {
		protected double PlanetRadius;
		protected LatLongPosition center;
		public LatLongToXYConverter(double planetRadius, LatLongPosition center) {
			this.PlanetRadius = planetRadius;
			this.center = center;
		}
		
		public LatLongPosition XYToLatLong(XYPosition xyPos) {
			throw new Exception("XYToLatLong not implemented");
		}
		
		public XYPosition LatLongToXY(LatLongPosition latLong) {
			// Given a latitude, make up a cylinder so that positions close to 0,0 are in the right direction-ish.
			// Will get screwy as you cos(latitude) approaches zero.
			double distanceFromAxis = PlanetRadius * Math.Cos(center.Latitude * Math.PI / 180);
			double longScale = distanceFromAxis * Math.PI / 180; // Size of one degree E/W, in xy units, at center
			double latScale = PlanetRadius * Math.PI / 180; // Size of one degree N/S, in xy units, at center
			return new XYPosition(
				(latLong.Longitude - center.Longitude) * longScale,
				(latLong.Latitude - center.Latitude) * latScale
			);
		}
	}

	class PointInfoConverter<Pos0,Pos1,Dat> {
		public delegate Pos1 PositionConverter(Pos0 input);

		protected PositionConverter positionConverter;

		public PointInfoConverter(PositionConverter positionConverter) {
			this.positionConverter = positionConverter;
		}

		public PointInfo<Pos1,Dat> ConvertPointPosition(PointInfo<Pos0,Dat> input) {
			return new PointInfo<Pos1, Dat>(positionConverter(input.Position), input.Data);
		}
	}
}
