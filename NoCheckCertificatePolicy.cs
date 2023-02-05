// From https://stackoverflow.com/questions/3674692/mono-webclient-invalid-ssl-certificates
// Mono doesn't trust root certs by default; this may be the quickest workaround.
// "Don't use this in production"

using System;
using System.Net;
using System.Security.Cryptography.X509Certificates;

namespace TOGoS.TScrpt34_2 {
   class NoCheckCertificatePolicy : ICertificatePolicy {
      public bool CheckValidationResult (ServicePoint srvPoint, X509Certificate certificate, WebRequest request, int certificateProblem) {
         return true;
      }
		
      public static void Init() {
         ServicePointManager.CertificatePolicy = new NoCheckCertificatePolicy ();
      }
   }
}
