# scalafun
Having fun using Scala and Akka.

## fscanner - super fast and efficient file-system scanner
* **Goal:** quickly scan recursively a monster file-system/directory
* **Challenges:**
  1. Since `java.nio.file.DirectoryStream` is synchronous, use multiple threads to scan directories in parallel. Also
  1. Memory usage should be kept low
* **Solution:**
  1. Use bluejeans's biqueue for storing child directories to be scanned in a queue off-heap
  1. Build an akka-stream that reads directories to be scanned from the queue then scanning them in a child graph
* **Summary:**
  1. the flow-control enforced by the stream results with as many open dir-iterators as `flatMapMerge` allows
  1. The `childrenSource` function creates a custom graph that performs IO operations asynchronously using futures