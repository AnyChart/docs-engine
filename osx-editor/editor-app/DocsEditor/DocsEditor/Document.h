//
//  Document.h
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 11/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <WebKit/WebKit.h>

@interface Document : NSDocument

@property (weak) IBOutlet WebView *preview;
@property (unsafe_unretained) IBOutlet NSTextView *textView;
- (IBAction)reload:(id)sender;

@end
