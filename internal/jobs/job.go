package jobs

import "context"

// Job is implemented by any background sync that runs until ctx is cancelled.
type Job interface {
	Run(ctx context.Context)
}
