require(DBI)
require(RMySQL)

select_all <- function(q) {
  r <- dbSendQuery(con, q)
  d <- fetch(r,n=-1)
  dbClearResult(r)
  d
}

drv <- dbDriver("MySQL")
con <- dbConnect(drv,
                 host="localhost",
                 dbname="gracilis",
                 username="arterioj",
                 password="arterioj",
                 unix.sock="/opt/local/var/run/mysql5/mysqld.sock")
