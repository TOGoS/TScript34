;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

(define (run-from-port port)
  (let ((line (read-line port)))
    (if (not (eof-object? line))
        (begin
          (display (string-append "read: " line "\n"))
          (run-from-port port)
          ))))

(define (parse-args args)
  (list (cons 'input-files args)))

(define (get-input-files config)
  (cdr (assoc 'input-files config)))

(define (main args)
  (let* ((config (parse-args args))
         (files (get-input-files config)))
    (if (null? files)
        (display "WARNING: no test case files were indicated\n")
        (for-each
          (lambda (file)
            (if (string=? file "-")
                (run-from-port (current-input-port))
                (call-with-input-file file run-from-port)))
          files))))
