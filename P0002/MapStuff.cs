using System.Collections.Generic;
using System.Collections.ObjectModel;

using Exception = System.Exception;
using JavaScriptConverter = System.Web.Script.Serialization.JavaScriptConverter;
using JavaScriptSerializer = System.Web.Script.Serialization.JavaScriptSerializer;
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
		readonly double Latitude;
		readonly double Longitude;
		public LatLongPosition(double lat, double longit) {
			this.Latitude = lat;
			this.Longitude = longit;
		}
		public override string ToString() {
			return "latitude: "+Latitude+", longitude: "+Longitude;
		}
	}
	readonly struct XYPosition {
		readonly double X;
		readonly double Y;
		public XYPosition(double x, double y) {
			this.X = x;
			this.Y = y;
		}
	}
	readonly struct VegData {
		readonly string KindName;
		public VegData(string kindName) {
			this.KindName = kindName;
		}
		public override string ToString() {
			return "kind: "+KindName;
		}
	}
	
	class LatLongVegJsConverter : JavaScriptConverter {
		public override IEnumerable<Type> SupportedTypes {
			//Define the ListItemCollection as a supported type.
			get { return new ReadOnlyCollection<Type>(new List<Type>(new Type[] { typeof(PointInfo<LatLongPosition,VegData>) })); }
		}
		
		public override object Deserialize(IDictionary<string, object> dictionary, Type type, JavaScriptSerializer serializer) {
			if (type == typeof(PointInfo<LatLongPosition,VegData>)) {
				return new PointInfo<LatLongPosition,VegData>(
					new LatLongPosition(
						serializer.ConvertToType<double>(dictionary["latitude"]),
						serializer.ConvertToType<double>(dictionary["longitude"])
					),
					new VegData(serializer.ConvertToType<string>(dictionary["kind"]))
				);
			}	
			return null;
		}
		
		public override IDictionary<string,object> Serialize(object obj, JavaScriptSerializer serializer) {
			throw new Exception("boo");
		}
	}
	
	class Decoder<Pos,Dat> {
		public IList<PointInfo<Pos,Dat>> Decode(string json) {
			var jser = new JavaScriptSerializer();
			// TODO: Something like this;
			// https://learn.microsoft.com/en-us/dotnet/api/system.web.script.serialization.javascriptserializer.registerconverters?view=netframework-4.8.1
			jser.RegisterConverters(new JavaScriptConverter[] {
				new LatLongVegJsConverter()
			});
			return jser.Deserialize<List<PointInfo<Pos,Dat>>>(json);
		}
	}
}
