#================================================================================
# Functions
#================================================================================

process_all <- function() {
  #proportion_strain()
  boxplot_all()
  #boxplot_range_all()
}

draw_hist <- function(x, title) {
  max_num <- max(x)
  print(paste("max_num", max_num))
  h <- hist(x, col=heat.colors(max_num), breaks=c(0, 1, 2, 3, 4, 5, 6),
       xlim=c(0, 6), ylim=c(0, 10),
       xlab="count/gracilis", ylab="Frequency", freq=T, right=F, main=title, border=grey(0.5))

  print(h)
}

proportion_strain <- function() {
  q <- paste("SELECT s.strain,i.name,count(*) as total, count(v.diameter) as n",
             " FROM vessel v,image i,subject s",
             " WHERE v.image_id=i.id",
             " AND (i.name=s.li OR i.name=s.ri)",
             " AND v.collateral=1",
#             " AND i.name=s.li",
             " GROUP BY i.name",
             " ORDER BY s.strain",
             sep="")
  #print(q)
  d <- select_all(q)
  diff <- d$total - d$n
  p <- diff / d$total
  missing <- ifelse(d$total != d$n, 1, 0)

  d <- cbind(d, diff, p, missing)
  #print(d)
  #summary(d)

  r <- aggregate(d, by=list(d$strain), mean)

  b <- subset(d, strain == 'balbc')
  c <- subset(d, strain == 'c57bl6')

  print(b)
  print(c)

  if (TRUE) {
    pdf("collaterals-histogram.pdf")
    par(mfrow=c(2,1), bg="black", fg="white", col="white", col.main="white", col.axis="white", col.lab="white") 
    draw_hist(b$n, "Number of collaterals (Balb/cJ)")
    draw_hist(c$n, "Number of collaterals (C57bl/6J)")
    dev.off()
  }
  
  if (TRUE) {
    pdf("missing-proportions-barplot.pdf")
    par(bg="black", fg="white", col="white", col.main="white", col.axis="white", col.lab="white") 

                                        #max_num <- max(d$diff)
    #hist(d$diff, col=heat.colors(max_num), breaks=max_num, xlim=c(0,max_num), right=F, main="Number of missing collaterals", las=1)

    #Hist(d$diff, freq=TRUE)
    x.abscis <- barplot(r[["missing"]],
                        names.arg=c("Balb/cJ", "C57bl/6J"),
                        col=c("white", gray(0.1)),
                        ylim=c(0,1),
                        cex.names=0.9,
                        main="Proportion of animals lacking one or more expected collaterals")
    dev.off()
  }


  #prop.test(b$missing, c$missing, p=0.5)
}
#height=10, width=14
boxplot_all <- function() {
  boxplot_metric("diameter", "microns", c(0, 25))
  print("------------")
  boxplot_metric("tortuosity", "k", c(0, 3))
  print("------------")
  boxplot_metric("ldr", "distance/length", c(1, 1.2))

  #pdf(paste("boxplots.pdf", sep="."))
  #par(mfrow=c(1,2), bg="black", fg="white", col="white", col.main="white", col.axis="white", col.lab="white") 
  #boxplot_vessel("li", "balbc", "diameter", "microns", c(0, 30), rgb(0, 84, 255, max=255))
  #boxplot_vessel("ri", "balbc", "diameter", "microns", c(0, 30), rgb(255, 85, 0, max=255))
  #boxplot_vessel("li", "c57bl6", "diameter", "microns", c(0, 30), rgb(0, 84, 255, max=255))
  #boxplot_vessel("ri", "c57bl6", "diameter", "microns", c(0, 30), rgb(255, 85, 0, max=255))
  #print("------------")
  #boxplot_vessel("li", "balbc", "tortuosity", "k", c(0, 3), rgb(0, 84, 255, max=255))
  #boxplot_vessel("ri", "balbc", "tortuosity", "k", c(0, 3), rgb(255, 85, 0, max=255))
  #boxplot_vessel("li", "c57bl6", "tortuosity", "k", c(0, 3), rgb(0, 84, 255, max=255))
  #boxplot_vessel("ri", "c57bl6", "tortuosity", "k", c(0, 3), rgb(255, 85, 0, max=255))
  #print("------------")
  
  #boxplot_vessel("li", "balbc", "ldr", "distance/length", c(0.0, 1.2), rgb(0, 84, 255, max=255))
  #boxplot_vessel("ri", "balbc", "ldr", "distance/length", c(0.0, 1.2), rgb(255, 85, 0, max=255))
  #boxplot_vessel("li", "c57bl6", "ldr", "distance/length", c(0.0, 1.2), rgb(0, 84, 255, max=255))
  #boxplot_vessel("ri", "c57bl6", "ldr", "distance/length", c(0.0, 1.2), rgb(255, 85, 0, max=255))

  #dev.off()
}

boxplot_metric <- function(metric, units, ylim) {
  pdf(paste("boxplots", metric, ".pdf", sep=""))
  par(mfrow=c(1,4), bg="black", fg="white", col="white", col.main="white", col.axis="white", col.lab="white") 
  boxplot_vessel("li", "balbc", metric, units, ylim, rgb(0, 84, 255, max=255))
  boxplot_vessel("ri", "balbc", metric, units, ylim, rgb(255, 85, 0, max=255))
  boxplot_vessel("li", "c57bl6", metric, units, ylim, rgb(0, 84, 255, max=255))
  boxplot_vessel("ri", "c57bl6", metric, units, ylim, rgb(255, 85, 0, max=255))
  dev.off()
}

boxplot_vessel <- function(side, strain, metric, ylab, ylim, color) {
  #q <- query_string(side, pod, range, strain, "not-used", metric)
  #q <- query_sum(side, pod, range, strain, "not-used", metric)
  q <- query_range(side, pod, 25, strain, "not-used", metric)
  print(q)
  d <- select_all(q)
  print(d)
  summary(d)
  title <- paste(strain, side, metric, sep="-")
  boxplot(metric~pod, data=d, xlab="POD", ylab=ylab, frame.plot=F, bty="n", ylim=ylim, col=color, show.names=TRUE, notch=FALSE, main=strain)
}


boxplot_path_range<- function(side, range, strain, metric, ylab, ylim) {
  q <- query_string_range(side, range, strain, metric)
  print(q)
  d <- select_all(q)
  print(d)
  summary(d)
  title <- paste(strain, side, metric, sep="-")
  #pdf(paste(title, ".range.pdf", sep="."), colormodel="cmyk")
  #boxplot(metric~pod, data=d, xlab="Post-operative day", ylab=ylab, ylim=ylim, show.names=TRUE, notch=FALSE, main=title)
  #dev.off()
}


boxplot_range_all <- function() {
  boxplot_path_range("li", 100, "balbc", "diameter", "microns", c(0, 30))
}

  #boxplot(tortuosity~name, data=d, names=paste(strain, pod, name, side, metric, sep="-"), show.names=TRUE, notch=FALSE, add=add)
  #boxplot(as.formula(paste(metric, "pod", sep="~")), data=d, xlab="Post-operative day", ylab=ylab, show.names=TRUE, notch=FALSE, main=paste(strain, side, metric, sep="-"), add=add)

query_string <- function(side, pod, range, strain, name, metric) {
  s = paste(
    "SELECT \"", side, "\"",
    ",s.id as 'sid'",
    ",s.strain",
    ",s.pod",
    ",i.name",
    ",v.", metric, " as metric", 
    " FROM vessel v,image i,subject s",
    " WHERE",
    " v.image_id=i.id",
    " AND v.collateral=1",
    " AND (v.label='agix' or v.label='agsix' or v.label='agssx')",
    " AND i.name=s.", side,
    " AND s.strain='",strain,"'",
    " ORDER BY s.pod",
    sep="")
}

query_sum <- function(side, pod, range, strain, name, metric) {
  s = paste(
    "SELECT \"", side, "\"",
    ",s.id as 'sid'",
    ",s.strain",
    ",s.pod",
    ",i.name",
    ",sum(v.", metric, ") as metric", 
    " FROM vessel v,image i,subject s",
    " WHERE",
    " v.image_id=i.id",
    " AND v.collateral=1",
    " AND (v.label='agix' or v.label='agsix' or v.label='agssx')",
    " AND i.name=s.", side,
    " AND s.strain='",strain,"'",
    " GROUP BY s.pod",
    sep="")
}

query_range <- function(side, pod, range, strain, name, metric) {
  s = paste(
    "SELECT \"", side, "\"",
    ",s.id as 'sid'",
    ",s.strain",
    ",s.pod",
    ",i.name",
    ",e.", metric, " as metric", 
    " FROM pathentry e, path p, vessel v,image i,subject s",
    " WHERE",
    " v.image_id=i.id",
    " AND e.path_id=p.id",
    " AND p.vessel_id=v.id",
    " AND (v.label='agix' or v.label='agsix' or v.label='agssx')",
    " AND i.name=s.", side,
    " AND s.strain='",strain,"'",
    " AND e.x<", range,
    " AND e.x>-", range,
    " ORDER BY e.seq",
    sep="")
}

query_string_range <- function(side, range, strain, metric) {
  print(metric)

  s = paste(
    "SELECT \"", side, "\"",
 #   ",count(*)",
    ",subject.id as 'sid'",
    ",subject.strain",
    ",subject.pod",
    ",image.name",
    ",path.id as 'pid'",
    ",path.name as pname",
    ",pathentry.id as 'eid'",
    ",pathentry.seq",
    ",pathentry.x",
#    ",sum(path.", metric, ")",
    ",pathentry.", metric,
    " FROM pathentry,path,vessel,image,subject",
#    " FROM path,vessel,image,subject",
    " WHERE",
    " pathentry.path_id=path.id",
    " AND path.vessel_id=vessel.id",
    " AND vessel.image_id=image.id",
    " AND image.name=subject.", side,
#    " AND subject.pod=",pod,
    " AND subject.strain='",strain,"'",
#    " AND path.name='",name,"'",
   " AND pathentry.x > -", range,
    " AND pathentry.x < ", range,
    " GROUP BY path.name",
    " ORDER BY x",
    sep="")
}


#================================================================================
# Main
#================================================================================

source("src/R/dbconnect.R")
process_all()
dbDisconnect(con)

