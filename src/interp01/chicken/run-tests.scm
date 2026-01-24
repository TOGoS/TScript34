;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

(define (run-test-cases-from-port port)
  (let ((line (read-line port)))
    (if (not (eof-object? line))
        (begin
          (display (string-append "read: " line "\n"))
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
