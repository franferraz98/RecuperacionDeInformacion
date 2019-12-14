import sys
import numpy as np
import csv
import matplotlib.pyplot as plt

with open('results.csv') as results:
    resReader = csv.reader(results, delimiter='\t')
    with open('qrels.csv') as qrels:
        qrelReader1 = csv.reader(qrels, delimiter='\t')
        qrelReader2 = csv.reader(qrels, delimiter='\t')
        ## ONE BY ONE
        all_lines1 = list(qrelReader1)
        all_lines2 = list(resReader)
        N = all_lines1[-2]
        retrieved = int(N[0])+1

        generalPrecision = 0
        generalRecall = 0
        generalAvg = 0
        generalInterpole = {}

        for index in range(1,retrieved):
            qrelMat = []
            resMat = []
            for row1 in all_lines1:
                if row1 != [] and int(row1[0]) == index and int(row1[2])==1:
                    qrelMat.append(row1[1])
                    for row2 in all_lines2:
                        if row2 != [] and int(row2[0]) == index:
                            resMat.append(row2[1])

            precisionMat = []
            recallMat = []

            interpole = {}
            maxPrecision = 0.0
            maxRecall = 0.0

            TP = 0
            FP = 0
            FN = 0
            for element in qrelMat:
                if element not in resMat:
                    FN += 1

            for element in resMat:
                if element in qrelMat:
                    TP += 1
                else:
                    FP +=1
                Precision = round(TP/(TP+FP),3)
                Recall = round(TP/(TP+FN),3)
                if Precision > maxPrecision and Precision < 1.0:
                    maxPrecision = Precision
                    interpole[Recall] = Precision
                if Recall > maxRecall and Recall < 1.0:
                    maxRecall = Recall
                    maxPrecision = 0.0
                precisionMat.append(Precision)
                recallMat.append(Recall)
            
            Precision = TP/(TP+FP)
            Recall = TP/(TP+FN)
            F1 = 2*(Precision*Recall)/(Precision+Recall)

            Precision_at_ten = precisionMat[9]
            totalPrecision = 0
            print(f'INFORMATION_NEED_{index}')
            print(f'precision: {Precision}')
            generalPrecision += Precision
            print(f'recall: {Recall}')
            generalRecall += Recall
            print(f'F1: {F1}')
            print(f'precision@10: {Precision_at_ten}')
            for i in range(0,len(precisionMat)):
                totalPrecision += precisionMat[i]
            avg_precision = totalPrecision/len(precisionMat)
            print(f'avg_precision {avg_precision}')
            generalAvg += avg_precision
            print(f'recall_precision')
            for i in range(0,len(precisionMat)):
                print(f'{recallMat[i]} {precisionMat[i]}')
            print(f'interpolated_recall_precision')
            keys = interpole.keys()
            for k in keys:
                print(f'{k} {interpole[k]}')
                if k not in generalInterpole or generalInterpole[k] > interpole[k]:
                    generalInterpole[k] = interpole[k]

        ## GENERAL
        print(f'TOTAL')
        print(f'precision {generalPrecision/retrieved}')
        print(f'recall {generalRecall/retrieved}')
        print(f'F1 {2*generalPrecision*generalRecall/(generalPrecision+generalRecall)}')
        maxPrecision = 0
        minRecall = 2
        print(f'interpolated_recall_precision')
        keys = generalInterpole.keys()
        pltkeys = []
        pltvalues = []
        for k in keys:
            print(f'{k} {generalInterpole[k]}')
            pltkeys.append(float(k))
            pltvalues.append(float(generalInterpole[k]))
            
        # plotting the points  
        plt.plot(pltkeys, pltvalues) 
        
        # naming the x axis 
        plt.xlabel('recall') 
        # naming the y axis 
        plt.ylabel('precision') 
        
        # giving a title to my graph 
        plt.title('Precision-Recall Curve') 
        
        # function to show the plot 
        plt.show() 
