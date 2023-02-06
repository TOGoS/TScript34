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
	}
	readonly struct LatLongPosition {
		readonly double Latitude;
		readonly double Longitude;
		public LatLongPosition(double lat, double longit) {
			this.Latitude = lat;
			this.Longitude = longit;
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
	}

	class Decoder<Pos,Dat> {
		IList<PointInfo<Pos,Dat>> Parse(string json) {
			var jser = new System.Web.Script.Serialization.JavaScriptSerializer();//<List<PointInfo<LatLongPosition,VegData>>>(json);
			// TODO: Read https://learn.microsoft.com/en-us/dotnet/api/system.web.script.serialization.javascriptserializer.deserialize
			throw new Exception("Not yet jajaja");
		}
	}
}
