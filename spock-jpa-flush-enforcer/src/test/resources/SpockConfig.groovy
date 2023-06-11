static boolean isExecutedFromIdea() {
    return System.getProperty("java.class.path").contains("idea_rt.jar")    //any more reliable way?
}

unroll {
    includeFeatureNameForIterations !isExecutedFromIdea()
}
