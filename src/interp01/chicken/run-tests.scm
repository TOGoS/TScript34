;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

(define (is-comment-or-blank? line)
  (irregex-match "^\\s*(?:#.*)?$" line))

(define tab-re (irregex "\t"))

(define-record-type test-case
  (make-test-case source-uri expected-result-uri)
  test-case?
  (source-uri test-case-source-uri)
  (expected-result-uri test-case-expected-result-uri))

(define (parse-test-case-tsv-line line)
  (if (is-comment-or-blank? line)
      '()
      (let* ((parts (irregex-split tab-re line)))
        (if (not (= (length parts) 2))
            (error (string-append "Malformed test case line: " line))
            (list (make-test-case (list-ref parts 0) (list-ref parts 1)))))))

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
      ((string=? "http://ns.nuke24.net/TOGVM/Functions/Concatenate" uri) string-append) ; Basically, for now
      ((string=? "urn:bitprint:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ.LWPNACQDBZRYXW3VHJVCJ64QBZNGHOHHHZWCLNQ" uri) "")
      ((string=? "urn:bitprint:SQ5HALIG6NCZTLXB7DNI56PXFFQDDVUZ.276TET7NAXG7FVCDQWOENOX4VABJSZ4GBV7QATQ" uri) "Hello, world!")
      ((string=? "urn:bitprint:ABJE2DLXTZHB436YIDSU7TUZDUC7H4MG.SIY4YBEH5BVCK54P5C3LKODI5V4L36EFH3XVONQ" uri) "|data:,|")
      (else (error (string-append "Idk how to fetch '" uri "'"))))))
;(define (load-blob uri)
;  (error (string-append "I don't know how to load " uri)))

;; TODO: Tests for load-blob

(define (eval-ts34p23-expression expr)
  (cond
    ((symbol? expr) (load-blob (symbol->string expr)))
    ((list? expr)
      (cond
        ((null? expr) (error "Can't eval empty list"))
        (else (let ((evaled-components (map eval-ts34p23-expression expr)))
          (eval evaled-components)))))
    (else (error "Can't even non-list, non-symbol"))))

(define (eval-ts34p23-expression-source expression-string)
  (let ((expr (read (open-input-string expression-string))))
    (eval-ts34p23-expression expr)))

; (display (string-append "Load..." (load-blob "data:,foo%20bar") "\n"))

(define-record-type test-result
  (make-test-result test-case passed actual-value error-messages)
  test-result?
  (test-case test-result-test-case)
  (passed test-result-passed)
  (actual-value test-result-actual-value)
  (error-messages test-result-error-messages))

(define (run-test-case test-case)
  (let* ((expression-source-uri (test-case-source-uri test-case))
         (expected-result-uri (test-case-expected-result-uri test-case))
         (expected-result (load-blob expected-result-uri))
         (expression-source (load-blob expression-source-uri))
         (result (eval-ts34p23-expression-source expression-source))
         (result-matches-expected (equal? result expected-result))
         (messages (if result-matches-expected '() (string-append "Expected {" expected-result "}, got {" result "} from {" expression-source "}"))))
    (make-test-result test-case result-matches-expected result messages)))

(define (run-test-cases-from-port port)
  (let ((line (read-line port)))
    (if (eof-object? line)
      '()
      (append
        (let ((test-cases (parse-test-case-tsv-line line)))
          (map run-test-case test-cases))
        (run-test-cases-from-port port)))))

(define (parse-args args)
  (list (cons 'input-files args)))

(define (get-input-files config)
  (cdr (assoc 'input-files config)))

(define (call-with-input-pseudo-file filename port-action)
  (if (string=? filename "-")
    (port-action (current-input-port))
    (call-with-input-file filename port-action)))

(define (run-test-cases-from-file file)
  (call-with-input-pseudo-file file run-test-cases-from-port))

(define (main args)
  (let* ((config (parse-args args))
         (files (get-input-files config)))
    (let* ((test-results (foldl (lambda (results file) (append results (run-test-cases-from-file file))) '() files))
           (_ (write test-results))
           (totals (foldl (lambda (totals test-result)
                            (let* ((failed-delta (if (test-result-passed test-result) 0 1))
                                   (passed-delta (- 1 failed-delta)))
                              (list (+ (car totals) failed-delta) (+ (cadr totals) passed-delta))))
                          (list 0 0)
                          test-results)))
     (display (string-append (number->string (car totals)) " failed, " (number->string (cadr totals)) " passed" "\n")))))
