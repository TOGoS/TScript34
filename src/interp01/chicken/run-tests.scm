;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

(define (run)
  (let ((line (read-line)))
    (if (not (eof-object? line))
        (begin
          (display (string-append "read: " line "\n"))
          (run)
          ))))

(define (main args)
  (run))
