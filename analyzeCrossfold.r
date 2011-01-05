data <- read.table('stewart/stewart.rtable')

names(data) = c("Value", "Fold", "Scheme")
attach(data)
Scheme=factor(Scheme)
## Fold=factor(Fold)

## jpeg('stewart.boxplot')
## boxplot(Value ~ Scheme)
## dev.off()

sink('stewart.analysis')
summary(fm1 <- aov(Value ~ Scheme))
TukeyHSD(fm1, "Scheme")
sink()
