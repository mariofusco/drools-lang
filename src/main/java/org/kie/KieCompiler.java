package org.kie;

import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ProcessTaskManager;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeCollisionException;
import org.eclipse.jdt.internal.compiler.util.Messages;

public class KieCompiler extends org.eclipse.jdt.internal.compiler.Compiler {
    public KieCompiler(INameEnvironment environment,
                       IErrorHandlingPolicy policy,
                       CompilerOptions options,
                       ICompilerRequestor requestor,
                       IProblemFactory problemFactory) {
        super(environment, policy, options, requestor, problemFactory);
    }

    @Override
    public void initializeParser() {
        this.parser = new KieParser(this.problemReporter, this.options.parseLiteralExpressionsAsConstants);
    }

    @Override
    public void compile(ICompilationUnit[] sourceUnits) {
        CompilationUnitDeclaration[] astUnits = generateASTs(sourceUnits);
        compileASTs(astUnits);
    }

    public CompilationUnitDeclaration[] generateASTs(ICompilationUnit[] sourceUnits) {
        this.stats.startTime = System.currentTimeMillis();

        // build and record parsed units
        reportProgress(Messages.compilation_beginningToCompile);

        if (this.annotationProcessorManager == null) {
            beginToCompile(sourceUnits);
        } else {
            ICompilationUnit[] originalUnits = (ICompilationUnit[]) sourceUnits.clone(); // remember source units in case a source type collision occurs
            try {
                beginToCompile(sourceUnits);
                processAnnotations();
            } catch (SourceTypeCollisionException e) {
                reset();
                // a generated type was referenced before it was created
                // the compiler either created a MissingType or found a BinaryType for it
                // so add the processor's generated files & start over,
                // but remember to only pass the generated files to the annotation processor
                int originalLength = originalUnits.length;
                int newProcessedLength = e.newAnnotationProcessorUnits.length;
                ICompilationUnit[] combinedUnits = new ICompilationUnit[originalLength + newProcessedLength];
                System.arraycopy(originalUnits, 0, combinedUnits, 0, originalLength);
                System.arraycopy(e.newAnnotationProcessorUnits, 0, combinedUnits, originalLength, newProcessedLength);
                this.annotationProcessorStartIndex  = originalLength;
                compile(combinedUnits);
            }
        }

        return this.unitsToProcess;
    }

    public void compileASTs(CompilationUnitDeclaration[] astUnits) {
        this.stats.startTime = System.currentTimeMillis();
        this.unitsToProcess = astUnits;
        this.totalUnits = astUnits.length;

        CompilationUnitDeclaration unit = null;
        ProcessTaskManager processingTask = null;
        try {
            if (this.useSingleThread) {
                // process all units (some more could be injected in the loop by the lookup environment)
                for (int i = 0; i < this.totalUnits; i++) {
                    unit = this.unitsToProcess[i];
                    reportProgress(Messages.bind(Messages.compilation_processing, new String(unit.getFileName())));
                    try {
                        if (this.options.verbose)
                            this.out.println(
                                    Messages.bind(Messages.compilation_process,
                                                  new String[] {
                                                          String.valueOf(i + 1),
                                                          String.valueOf(this.totalUnits),
                                                          new String(this.unitsToProcess[i].getFileName())
                                                  }));
                        process(unit, i);
                    } finally {
                        // cleanup compilation unit result
                        unit.cleanUp();
                    }
                    this.unitsToProcess[i] = null; // release reference to processed unit declaration

                    reportWorked(1, i);
                    this.stats.lineCount += unit.compilationResult.lineSeparatorPositions.length;
                    long acceptStart = System.currentTimeMillis();
                    this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
                    this.stats.generateTime += System.currentTimeMillis() - acceptStart; // record accept time as part of generation
                    if (this.options.verbose)
                        this.out.println(
                                Messages.bind(Messages.compilation_done,
                                              new String[] {
                                                      String.valueOf(i + 1),
                                                      String.valueOf(this.totalUnits),
                                                      new String(unit.getFileName())
                                              }));
                }
            } else {
                processingTask = new ProcessTaskManager(this);
                int acceptedCount = 0;
                // process all units (some more could be injected in the loop by the lookup environment)
                // the processTask can continue to process units until its fixed sized cache is full then it must wait
                // for this this thread to accept the units as they appear (it only waits if no units are available)
                while (true) {
                    try {
                        unit = processingTask.removeNextUnit(); // waits if no units are in the processed queue
                    } catch (Error e) {
                        unit = processingTask.unitToProcess;
                        throw e;
                    } catch (RuntimeException e) {
                        unit = processingTask.unitToProcess;
                        throw e;
                    }
                    if (unit == null) break;
                    reportWorked(1, acceptedCount++);
                    this.stats.lineCount += unit.compilationResult.lineSeparatorPositions.length;
                    this.requestor.acceptResult(unit.compilationResult.tagAsAccepted());
                    if (this.options.verbose)
                        this.out.println(
                                Messages.bind(Messages.compilation_done,
                                              new String[] {
                                                      String.valueOf(acceptedCount),
                                                      String.valueOf(this.totalUnits),
                                                      new String(unit.getFileName())
                                              }));
                }
            }
        } catch (org.eclipse.jdt.internal.compiler.problem.AbortCompilation e) {
            this.handleInternalException(e, unit);
        } catch (Error e) {
            this.handleInternalException(e, unit, null);
            throw e; // rethrow
        } catch (RuntimeException e) {
            this.handleInternalException(e, unit, null);
            throw e; // rethrow
        } finally {
            if (processingTask != null) {
                processingTask.shutdown();
                processingTask = null;
            }
            reset();
            this.annotationProcessorStartIndex  = 0;
            this.stats.endTime = System.currentTimeMillis();
        }
        if (this.options.verbose) {
            if (this.totalUnits > 1) {
                this.out.println(
                        Messages.bind(Messages.compilation_units, String.valueOf(this.totalUnits)));
            } else {
                this.out.println(
                        Messages.bind(Messages.compilation_unit, String.valueOf(this.totalUnits)));
            }
        }
    }
}
