;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

;; TestCase = ('test-case input-uri expected-result-uri)

(define (is-comment-or-blank? line)
  (irregex-match "^\\s*(?:#.*)?$" line))

(define (string-split str delimiter)
  (irregex-split (irregex delimiter) str))

(define (parse-test-case-tsv-line line callback)
  (if (not (is-comment-or-blank? line))
      (let* ((parts (string-split line "\t")))
        (if (not (= (length parts) 2))
            (error (string-append "Malformed test case line: " line))
            (callback `(test-case ,(list-ref parts 0) ,(list-ref parts 1)))))))

(define (run-test-case test-case)
  (display (string-append "test case: input URI = " (cadr test-case) ", expected result = " (caddr test-case) "\n")))

(define (run-test-cases-from-port port)
  (let ((line (read-line port)))
    (if (not (eof-object? line))
        (begin
          (parse-test-case-tsv-line line run-test-case)
          (run-test-cases-from-port port)
          ))))

(define (parse-args args)
  (list (cons 'input-files args)))

(define (get-input-files config)
  (cdr (assoc 'input-files config)))

(define (call-with-input-pseudo-file filename port-action)
  (if (string=? filename "-")
    (port-action (current-input-port))
    (call-with-input-file filename port-action)))

(define (run-test-cases-from-file file) (call-with-input-pseudo-file file run-test-cases-from-port))

(define (main args)
  (let* ((config (parse-args args))
         (files (get-input-files config)))
    (if (null? files)
        (display "WARNING: no test case files were indicated\n")
        (for-each run-test-cases-from-file files))))
