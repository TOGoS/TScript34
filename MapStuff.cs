using Exception = System.Exception;
using System.Collections.Generic;

namespace TOGoS.TScrpt34_2.MapStuff {
	readonly struct PointInfo<Pos,Dat> {
		public readonly Pos Position;
		public readonly Dat Data;
		public PointInfo(Pos position, Dat data) {
			this.Position = position;
			this.Data = data;
		}
		public override string ToString() {
			return "PointInfo { "+Position+", "+Data+"}";
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
	
	class Decoder<Pos,Dat> {
		public IList<PointInfo<Pos,Dat>> Decode(string json) {
			var jser = new System.Web.Script.Serialization.JavaScriptSerializer();
			// TODO: Something like this;
			// https://learn.microsoft.com/en-us/dotnet/api/system.web.script.serialization.javascriptserializer.registerconverters?view=netframework-4.8.1
			// jser.RegisterConverters(new JavaScriptConverter[] {});
			return jser.Deserialize<List<PointInfo<Pos,Dat>>>(json);
		}
	}
}
