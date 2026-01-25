;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

;; TestCase = ('test-case input-uri expected-result-uri)

(define (is-comment-or-blank? line)
  (irregex-match "^\\s*(?:#.*)?$" line))

(define tab-re (irregex "\t"))

(define (parse-test-case-tsv-line line callback)
  (if (not (is-comment-or-blank? line))
      (let* ((parts (irregex-split tab-re line)))
        (if (not (= (length parts) 2))
            (error (string-append "Malformed test case line: " line))
            (callback `(test-case ,(list-ref parts 0) ,(list-ref parts 1)))))))

(define data-uri-re (irregex "data:,(.*)"))
(define (hex-decode hex)
  (string (integer->char (string->number hex 16))))
(define (uri-decode encoded)
  (irregex-replace/all "%([0-9A-Fa-f][0-9A-Fa-f])" encoded
    (lambda (match-data) (hex-decode (irregex-match-substring match-data 1)))))

;; TODO: Tests for uri-decode
    
(define (load-blob uri)
  ; (display (string-append "Loading " uri "...\n"))  
  (let ((match-data (irregex-match data-uri-re uri)))
    (cond
      (match-data (uri-decode (irregex-match-substring match-data 1)))
      ((string=? "urn:bitprint:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ.LWPNACQDBZRYXW3VHJVCJ64QBZNGHOHHHZWCLNQ" uri) "")
      ((string=? "urn:bitprint:SQ5HALIG6NCZTLXB7DNI56PXFFQDDVUZ.276TET7NAXG7FVCDQWOENOX4VABJSZ4GBV7QATQ" uri) "Hello, world!")
      ((string=? "urn:bitprint:ABJE2DLXTZHB436YIDSU7TUZDUC7H4MG.SIY4YBEH5BVCK54P5C3LKODI5V4L36EFH3XVONQ" uri) "|data:,|")
      (else (error (string-append "Idk how to fetch '" uri "'"))))))
;(define (load-blob uri)
;  (error (string-append "I don't know how to load " uri)))

;; TODO: Tests for load-blob

(define quoted-symbol-re (irregex "\\|(.*)\\|"))

(define (eval-ts34p23-expression expression-string)
    ;; TODO: Let Chicken's reader do the parsing.
    (let* (
      (quoted-symbol-match-data (irregex-match quoted-symbol-re expression-string))
      (the-symbol (if quoted-symbol-match-data
          (irregex-match-substring quoted-symbol-match-data 1) ;; TODO: Unescape '\'+whatevers
          expression-string))
    ) (load-blob the-symbol)))
  
; (display (string-append "Load..." (load-blob "data:,foo%20bar") "\n"))

(define (run-test-case test-case)
  (let* ((expression-source-uri (cadr test-case))
         (expected-result-uri (caddr test-case))
         (expected-result (load-blob expected-result-uri))
         (expression-source (load-blob expression-source-uri)))
  ; (display (string-append "test case: input URI = " expression-source-uri ", expected result = " expected-result-uri  "\n"))
    (let ((result (eval-ts34p23-expression expression-source)))
      (if (not (equal? result expected-result))
        (error (string-append "Expected {" expected-result "}, got {" result "} from {" expression-source "}"))))))

(define (run-test-cases-from-port port running-test-count)
  (let ((line (read-line port)))
    (if (eof-object? line)
      running-test-count
      (begin
        (parse-test-case-tsv-line line run-test-case)
        (run-test-cases-from-port port (+ 1 running-test-count))))))

(define (parse-args args)
  (list (cons 'input-files args)))

(define (get-input-files config)
  (cdr (assoc 'input-files config)))

(define (call-with-input-pseudo-file filename port-action)
  (if (string=? filename "-")
    (port-action (current-input-port))
    (call-with-input-file filename port-action)))

(define (run-test-cases-from-file file) (call-with-input-pseudo-file file (lambda (port) (run-test-cases-from-port port 0))))

(define (main args)
  (let* ((config (parse-args args))
         (files (get-input-files config)))
    (if (null? files)
        (display "WARNING: no test case files were indicated\n")
        (let ((test-result (foldl (lambda (c f) (+ c (run-test-cases-from-file f))) 0 files)))
          (display (string-append (number->string test-result) " tests passed!\n"))))))
