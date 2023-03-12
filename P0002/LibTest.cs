// Demonstrate using TScript34_2 as a library.

namespace TOGoS.TScrpt34_2 {
    class LibTest {
        class LibTestOp : Op {
            public void Do(Interpreter interp) {
                interp.PushThunk(interp.ValueToThunk("LibTest"));
            }
        }
        public static void Main(string[] args) {
            Interpreter interp = new Interpreter();
            interp.Definitions.Add("xxx:LibTest", new LibTestOp());
            interp.DefineAll(StandardOps.Definitions);
            interp.DoMain(args);
        }
    }
}